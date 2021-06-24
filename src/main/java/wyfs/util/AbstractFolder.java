// Copyright 2011 The Whiley Project Developers
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package wyfs.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import wybs.lang.Build;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.lang.Content.Filter;
import wyfs.lang.Content.Type;
import wyfs.lang.Path.Entry;
import wyfs.lang.Path.ID;

/**
 * An abstract folder contains other folders, and path entries. As such, it
 * cannot be considered a concrete entry which can be read and written in the
 * normal manner. Rather, it provides access to entries. In a physical file
 * system, a folder would correspond to a directory.
 *
 * @author David J. Pearce
 *
 */
public abstract class AbstractFolder implements Path.Folder {
	protected final Path.ID id;
	private Path.Item[] contents;
	private int nentries;

	/**
	 * Construct an Abstract Folder representing a given ID (taken relative to the
	 * enclosing root).
	 *
	 * @param id
	 */
	public AbstractFolder(Path.ID id) {
		this.id = id;
	}

	@Override
	public Path.ID id() {
		return id;
	}

	@Override
	public boolean contains(Path.Entry<?> e) throws IOException {
		updateContents();
		//
		Path.ID eid = e.id();

		int idx = binarySearch(contents, nentries, eid);
		if (idx >= 0) {
			// At this point, we've found a matching index for the given ID.
			// However, there maybe multiple matching IDs (e.g. with different
			// content types). Therefore, we need to check them all to see if
			// they match the requested entry.
			Path.Item item = contents[idx];
			do {
				if (item == e) {
					return true;
				} else if (item instanceof Path.Folder && eid.size() > id.size()) {
					Path.Folder folder = (Path.Folder) item;
					return folder.contains(e);
				}
			} while (++idx < nentries && (item = contents[idx]).id().equals(eid));
		}

		// no dice
		return false;
	}

	@Override
	public boolean exists(ID id, Content.Type<?> ct) throws IOException {
		return get(id, ct) != null;
	}

	@Override
	public <T> Path.Entry<T> get(ID eid, Content.Type<T> ct) throws IOException {
		updateContents();
		final int id_size = id().size();
		//
		int idx = binarySearch(contents, nentries, eid);
		if (idx >= 0) {
			// At this point, we've found a matching index for the given ID.
			// However, there maybe multiple matching IDs with different
			// content types. Therefore, we need to check them all to see if
			// they match the requested entry.
			Path.Item item = contents[idx];
			do {
				if (item instanceof Entry && eid.equals(item.id())) {
					// In this case, we're looking for and have found an exact
					// item.
					Entry entry = (Entry) item;
					if (entry.contentType() == ct) {
						return entry;
					}
				} else if (item instanceof Path.Folder && eid.size() > id_size) {
					// In this case, the ID is indicates the item is not
					// contained in this folder.
					Path.Folder folder = (Path.Folder) item;
					Path.Entry<T> entry = folder.get(eid, ct);
					if(entry != null) {
						return entry;
					}
				}
			} while (++idx < nentries && (item = contents[idx]).id().equals(eid));
		}

		// no dice
		return null;
	}

	@Override
	public List<Entry<?>> getAll() throws IOException {
		updateContents();
		ArrayList entries = new ArrayList();

		// It would be nice to further optimise this loop. Basically, to avoid
		// creating so many ArrayList objects. However, it's tricky to get right
		// given Java's generic type system.

		for (int i = 0; i != nentries; ++i) {
			Path.Item item = contents[i];
			if (item instanceof Entry) {
				Entry entry = (Entry) item;
				entries.add(entry);
			} else if (item instanceof Path.Folder) {
				Path.Folder folder = (Path.Folder) item;
				entries.addAll(folder.getAll());
			}
		}

		return entries;
	}

	@Override
	public <T> void getAll(Content.Filter<T> filter, List<Entry<T>> entries) throws IOException {
		updateContents();
		// It would be nice to further optimise this loop. The key issue is that,
		// at some point, we might know the filter could never match. In which
		// case, we want to stop the recursion early, rather than exploring a
		// potentially largel subtree.
		for (int i = 0; i != nentries; ++i) {
			Path.Item item = contents[i];
			if (item instanceof Entry) {
				Entry entry = (Entry) item;
				if (filter.matches(entry.id(), entry.contentType())) {
					entries.add(entry);
				}
			} else if (item instanceof Path.Folder && filter.matchesSubpath(item.id())) {
				Path.Folder folder = (Path.Folder) item;
				folder.getAll(filter, entries);
			}
		}
	}

	@Override
	public <T> void getAll(Content.Filter<T> filter, Set<Path.ID> entries) throws IOException {
		updateContents();
		// It would be nice to further optimise this loop. The key issue is that,
		// at some point, we might know the filter could never match. In which
		// case, we want to stop the recursion early, rather than exploring a
		// potentially largel subtree.
		for (int i = 0; i != nentries; ++i) {
			Path.Item item = contents[i];
			if (item instanceof Entry) {
				Entry entry = (Entry) item;
				if (filter.matches(entry.id(), entry.contentType())) {
					entries.add(entry.id());
				}
			} else if (item instanceof Path.Folder && filter.matchesSubpath(item.id())) {
				Path.Folder folder = (Path.Folder) item;
				folder.getAll(filter, entries);
			}
		}
	}

	@Override
	public void refresh() throws IOException {
		if(contents != null) {
			// Extract contents
			Path.Item[] items = contents();
			// Sort them
			Arrays.sort(items, entryComparator);
			// Proceed to update contents
			int oj = 0;
			int ni = 0;
			while(oj < nentries && ni < items.length) {
				Path.Item o = contents[oj];
				Path.Item n = items[ni];
				int cmp = o.id().compareTo(n.id());
				if (cmp > 0) {
					// Old item after new item. This indicates a previously unseen item. Therefore,
					// insert into contents.
					insert(oj, n);
				} else if(cmp < 0) {
					// New item after old item. This indicates an item which either did not
					// originally exist, or has been deleting by some external process. Therefore,
					// we simply skip it.
					ni = ni - 1;
				}
				oj = oj + 1;
				ni = ni + 1;
			}
			// Anything remaining is new and therefore should be inserted.
			while (ni < items.length) {
				insert(oj++, items[ni++]);
			}
			// Recursively refresh everything
			for(int i=0;i!=nentries;++i) {
				contents[i].refresh();
			}
		}
	}


	@Override
	public void flush() throws IOException {
		if(contents != null) {
			for (int i = 0; i != nentries; ++i) {
				contents[i].flush();
			}
		}
	}

	@Override
	public boolean remove(ID id, Type<?> ct) throws IOException {
		updateContents();
		// Find start of matches
		int index = binarySearch(contents, nentries, id);
		// Attempt to find item with matching content type
		index = match(index, contents, nentries, id, ct);
		// Did we find anything?
		if (index >= 0) {
			// shift everything down one
			for (int i = index + 1; i < nentries; ++i) {
				contents[i - 1] = contents[i];
			}
			nentries = nentries - 1;
			contents[nentries] = null;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int remove(Filter<?> filter) throws IOException {
		updateContents();
		int count = 0;
		//
		for (int i = 0; i != nentries; ++i) {
			//
			Path.Item item = contents[i];
			//
			if (item instanceof Entry) {
				Entry entry = (Entry) item;
				if (filter.matches(entry.id(), entry.contentType()) && remove(entry.id(), entry.contentType())) {
					count = count + 1;
					i = i - 1;
				}
			} else if (item instanceof Path.Folder && filter.matchesSubpath(item.id())) {
				Path.Folder folder = (Path.Folder) item;
				count += folder.remove(filter);
			}
		}
		//
		return count;
	}

	protected Path.Folder getFolder(String name) throws IOException {
		updateContents();
		ID tid = id.append(name);

		int idx = binarySearch(contents, nentries, tid);
		if (idx >= 0) {
			// At this point, we've found a matching index for the given ID.
			// However, there maybe multiple matching IDs with different
			// content types. Therefore, we need to check them all to see if
			// they match the requested entry.
			Path.Item item = contents[idx];
			do {
				if (item instanceof Path.Folder) {
					// In this case, the ID is indicates the item is not
					// contained in this folder.
					return (Path.Folder) item;
				}
			} while (++idx < nentries && (item = contents[idx]).id().equals(tid));
		}

		// no dice
		return null;
	}

	/**
	 * Insert a newly created item into this folder. Observe we assume
	 * <code>entry.id().parent() == id</code>.
	 *
	 * @param item
	 */
	protected void insert(Path.Item item) throws IOException {
		if (item.id().parent() != id) {
			throw new IllegalArgumentException(
					"Cannot insert with incorrect Path.Item (" + item.id() + ") into AbstractFolder (" + id + ")");
		}
		updateContents();
		//
		Path.ID id = item.id();
		int index = binarySearch(contents, nentries, id);

		if (index < 0) {
			index = -index - 1; // calculate insertion point
		} else {
			// indicates already an entry with a different content type
		}
		// Insert item at given index
		insert(index,item);
	}

	private void insert(int index, Path.Item item) {
		// Check whether sufficient space remaining
		if ((nentries + 1) < contents.length) {
			// Yes, move all items up the index
			System.arraycopy(contents, index, contents, index + 1, nentries - index);
		} else {
			Path.Item[] tmp = new Path.Item[(nentries + 1) * 2];
			System.arraycopy(contents, 0, tmp, 0, index);
			System.arraycopy(contents, index, tmp, index + 1, nentries - index);
			contents = tmp;
		}

		contents[index] = item;
		nentries++;
	}

	/**
	 * Extract all entries from the given folder.
	 */
	protected abstract Path.Item[] contents() throws IOException;

	private static final int binarySearch(final Path.Item[] children, int nchildren, final Path.ID key) {
		int low = 0;
		int high = nchildren - 1;

		while (low <= high) {
			int mid = (low + high) >> 1;
			int c = children[mid].id().compareTo(key);
			if (c < 0) {
				low = mid + 1;
			} else if (c > 0) {
				high = mid - 1;
			} else {
				// found a batch, locate start point
				mid = mid - 1;
				while (mid >= 0 && children[mid].id().compareTo(key) == 0) {
					mid = mid - 1;
				}
				return mid + 1;
			}
		}
		return -(low + 1);
	}

	private final void updateContents() throws IOException {
		if (contents == null) {
			contents = contents();
			nentries = contents.length;
			Arrays.sort(contents, entryComparator);
		}
	}

	private int match(int start, final Path.Item[] children, int nchildren, final Path.ID key,
			final Content.Type type) {
		while (contents[start].id().equals(key)) {
			Path.Item item = contents[start];
			if (item instanceof Path.Entry) {
				Path.Entry entry = (Path.Entry) item;
				if (entry.contentType().equals(type)) {
					return start;
				}
			}
			start = start + 1;
		}
		return -1;
	}

	private static final Comparator<Path.Item> entryComparator = new Comparator<Path.Item>() {
		@Override
		public int compare(Path.Item e1, Path.Item e2) {
			return e1.id().compareTo(e2.id());
		}
	};
}
