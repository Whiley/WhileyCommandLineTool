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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.lang.Content.Filter;
import wyfs.lang.Content.Type;
import wyfs.lang.Path.Entry;
import wyfs.lang.Path.Folder;
import wyfs.lang.Path.ID;
import wyfs.lang.Path.RelativeRoot;
import wyfs.lang.Path.Root;
import wyfs.util.AbstractRoot.Relative;

/**
 * Provides a simple implementation of <code>Path.Root</code>. This maintains a
 * cache all entries contained in the root.
 *
 * @author David J. Pearce
 *
 */
public abstract class AbstractRoot<T extends Folder> implements Root {
	protected final Content.Registry contentTypes;
	protected final T root;

	public AbstractRoot(Content.Registry contentTypes) {
		this.contentTypes = contentTypes;
		this.root = root();
	}

	public AbstractRoot(Content.Registry contentTypes, T root) {
		this.contentTypes = contentTypes;
		this.root = root;
	}

	@Override
	public boolean contains(Path.Entry<?> e) throws IOException {
		return root.contains(e);
	}

	@Override
	public boolean exists(ID id, Content.Type<?> ct) throws IOException{
		return root.exists(id,ct);
	}

	@Override
	public <T> Path.Entry<T> get(ID id, Content.Type<T> ct) throws IOException{
		Path.Entry<T> e = root.get(id,ct);
		return e;
	}

	@Override
	public <T> List<Entry<T>> get(Content.Filter<T> filter) throws IOException{
		ArrayList<Entry<T>> entries = new ArrayList<>();
		root.getAll(filter, entries);
		return entries;
	}

	@Override
	public <T> Set<Path.ID> match(Content.Filter<T> filter) throws IOException{
		HashSet<Path.ID> ids = new HashSet<>();
		root.getAll(filter, ids);
		return ids;
	}

	@Override
	public <T> Path.Entry<T> create(Path.ID id, Content.Type<T> ct) throws IOException {
		return root.create(id,ct);
	}

	@Override
	public final RelativeRoot createRelativeRoot(ID id) throws IOException {
		return new Relative(this,id);
	}

	@Override
	public boolean remove(ID id, Type<?> ct) throws IOException {
		return root.remove(id,ct);
	}

	@Override
	public int remove(Filter<?> cf) throws IOException {
		return root.remove(cf);
	}

	@Override
	public void refresh() throws IOException{
		root.refresh();
	}

	@Override
	public void flush() throws IOException{
		root.flush();
	}

	/**
	 * Get the root folder for this abstract root. Note that this should be
	 * loaded from scratch, and not cached in any way. This ensures that
	 * invoking AbstractRoot.refresh() does indeed refresh entries.
	 *
	 * @return
	 */
	protected abstract T root();

	public final static class Relative implements Path.RelativeRoot {
		private final Path.Root parent;
		private final ID prefix;

		public Relative(Path.Root parent, ID prefix) throws IOException {
			if(prefix == null) {
				throw new IllegalArgumentException("prefix cannot be null");
			}
			this.parent = parent;
			this.prefix = prefix;
		}

		@Override
		public Root getParent() {
			return parent;
		}

		@Override
		public boolean contains(Path.Entry<?> entry) throws IOException {
			// FIXME: unsure whether this makes sense or not
			return parent.contains(entry);
		}

		@Override
		public boolean exists(ID id, Type<?> ct) throws IOException {
			return parent.exists(prefix.append(id), ct);
		}

		@Override
		public <T> Path.Entry<T> get(ID id, Type<T> ct) throws IOException {
			return parent.get(prefix.append(id),ct);
		}

		@Override
		public <T> List<wyfs.lang.Path.Entry<T>> get(Filter<T> cf) throws IOException {
			return parent.get(new RelativeFilter<>(prefix, cf));
		}

		@Override
		public <T> Set<ID> match(Filter<T> cf) throws IOException {
			return parent.match(new RelativeFilter<>(prefix, cf));
		}

		@Override
		public <T> wyfs.lang.Path.Entry<T> create(ID id, Type<T> ct) throws IOException {
			return parent.create(prefix.append(id), ct);
		}

		@Override
		public boolean remove(ID id, Type<?> ct) throws IOException {
			return parent.remove(prefix.append(id), ct);
		}

		@Override
		public int remove(Filter<?> cf) throws IOException {
			return parent.remove(new RelativeFilter<>(prefix, cf));
		}

		@Override
		public RelativeRoot createRelativeRoot(ID id) throws IOException {
			return new Relative(parent,prefix.append(id));
		}

		@Override
		public void flush() throws IOException {
			parent.flush();
		}

		@Override
		public void refresh() throws IOException {
			parent.refresh();
		}

		@Override
		public String toString() {
			return parent.toString() + "::" + prefix;
		}
	}

	private static final class RelativeFilter<T> implements Content.Filter<T> {
		private final ID prefix;
		private final Content.Filter<T> filter;

		public RelativeFilter(ID prefix, Content.Filter<T> filter) {
			if(prefix == null) {
				throw new IllegalArgumentException("prefix cannot be null");
			}
			this.prefix = prefix;
			this.filter = filter;
		}

		@Override
		public boolean matches(ID id, Type<T> ct) {
			if (id.size() >= prefix.size()) {
				Path.ID id_prefix = id.subpath(0, prefix.size());
				if (id_prefix.equals(prefix)) {
					Path.ID id_suffix = id.subpath(prefix.size(), id.size());
					return filter.matches(id_suffix,ct);
				}
			}
			return false;
		}

		@Override
		public boolean matchesSubpath(ID id) {
			if (id.size() < prefix.size()) {
				return prefix.subpath(0, id.size()).equals(id);
			} else {
				Path.ID id_prefix = id.subpath(0, prefix.size());
				if (id_prefix.equals(prefix)) {
					Path.ID id_suffix = id.subpath(prefix.size(), id.size());
					return filter.matchesSubpath(id_suffix);
				}
			}
			return false;
		}

	}
}