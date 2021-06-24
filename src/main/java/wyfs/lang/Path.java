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
package wyfs.lang;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.*;

public class Path {

	/**
	 * Represents a sequence of zero or more names which describe a path through
	 * the namespace for a given project. For example, "whiley/lang/Math" is a
	 * valid ID with three components: "whiley","lang","Math".
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface ID extends Iterable<String>, Comparable<ID> {

		/**
		 * Get the number of components that make up this ID.
		 * @return
		 */
		public int size();

		/**
		 * Return the component at a given index.
		 * @param index
		 * @return
		 */
		public String get(int index);

		/**
		 * A convenience function that gets the last component of this path.
		 *
		 * @return
		 */
		public String last();

		/**
		 * Get the parent of this path.
		 *
		 * @return
		 */
		public ID parent();

		/**
		 * Get a sub ID from this id, which consists of those components between
		 * start and end (exclusive).
		 *
		 * @param start
		 *            --- starting component index
		 * @param start
		 *            --- one past last component index
		 * @return
		 */
		public ID subpath(int start, int end);

		/**
		 * Append a component onto the end of this id.
		 *
		 * @param component
		 *            --- to be appended
		 * @return
		 */
		public ID append(String component);

		/**
		 * Append all components from an ID onto the end of this ID.
		 *
		 * @param id
		 * @return
		 */
		public ID append(ID id);
	}

	/**
	 * Represents an abstract or physical item of some sort which is reachable
	 * from a <code>Root</code>. Valid instances of <code>Item</code> include
	 * those valid instances of <code>Entry</code> and <code>Folder</code>.
	 */
	public interface Item {
		/**
		 * Return the identify of this item.
		 *
		 * @return
		 */
		public ID id();

		/**
		 * Force item to refresh contents from permanent storage (where
		 * appropriate). For items which have been modified, this operation has
		 * no effect (i.e. the new contents are retained). For folders, this
		 * forces sub-folders to be refreshed as well.
		 */
		public void refresh() throws IOException;

		/**
		 * Force item to write contents to permanent storage (where
		 * appropriate). For items which have not been modified, this operation
		 * has no effect (i.e. the old contents are retained). For folers, this
		 * forces sub-folders to be flushed as well.
		 */
		public void flush() throws IOException;
	}

	/**
	 * Represents a physical item of some sort which is reachable from a
	 * <code>Root</code>. Valid instances of <code>Entry</code> may correspond
	 * to files on the file system, entries in a Jar file, or abstractions from
	 * other tools (e.g. eclipse's <code>IFile</code>).
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Entry<T> extends Item {

		/**
		 * Return the suffix of the item in question. This is necessary to
		 * determine how we will process this item.
		 *
		 * @return
		 */
		public String suffix();

		/**
		 * Return a string indicating the true location of this entry.
		 *
		 * @return
		 */
		public String location();

		/**
		 * Get the last modification time for this file.
		 *
		 * @return
		 */
		public long lastModified();

		/**
		 * Check whether this file has been modified or not.
		 *
		 * @return
		 */
		public boolean isModified();

		/**
		 * Mark this entry as being modified.
		 *
		 * @return
		 */
		public void touch();

		/**
		 * Get the content type associated with this file. This provides a
		 * generic mechanism for describing the information contained within the
		 * file.
		 */
		public Content.Type<T> contentType();

		/**
		 * Associate this entry with a content type, and optionally provide the
		 * contents. The ability to provide the contents is a convenience
		 * function for cases where determining the content type requires
		 * actually reading the contents!
		 *
		 * @param contentType
		 *            --- content type to associate
		 * @param contents
		 *            --- contents to associate, or null if none.
		 */
		public void associate(Content.Type<T> contentType, T contents);

		/**
		 * Read contents of file. Note, however, that this does not mean the
		 * contents are re-read from permanent storage. If the contents are
		 * already available in memory, then they will returned without
		 * accessing permanent storage.
		 */
		public T read() throws IOException;

		/**
		 * Write the contents of this entry. It is assumed that the contents
		 * matches the content-type given for this entry. Finally, note also
		 * that this does not mean the contents are written to permanent
		 * storage.
		 *
		 * @param contents
		 */
		public void write(T contents) throws IOException;

		/**
		 * Open a generic input stream to the entry.
		 *
		 * @return
		 * @throws IOException
		 */
		public InputStream inputStream() throws IOException;

		/**
		 * Open a generic output stream to the entry.
		 *
		 * @return
		 * @throws IOException
		 */
		public OutputStream outputStream() throws IOException;
	}

	/**
	 * An folder represents a special kind of entry which contains entries (and
	 * other folders). As such, it cannot be considered a concrete entry which
	 * can be read and written in the normal manner. Rather, it provides access
	 * to entries. For example, in a physical file system, a folder would
	 * correspond to a directory.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Folder extends Item {

		/**
		 * Check whether or not a given entry is contained in this folder;
		 *
		 * @param entry
		 * @return
		 */
		public boolean contains(Path.Entry<?> entry) throws IOException;

		/**
		 * folder) and content-type is contained in this folder.
		 *
		 * @throws IOException
		 *             --- in case of some I/O failure.
		 */
		public boolean exists(ID id, Content.Type<?> ct) throws IOException;

		/**
		 * Get the entry corresponding to a given ID and content type. If no such entry
		 * exists, return null.
		 *
		 * @param id --- id of module to lookup.
		 * @throws IOException --- in case of some I/O failure.
		 */
		public <T> Path.Entry<T> get(ID id, Content.Type<T> ct)
				throws IOException;

		/**
		 * Get all objects contained in this folder (including those contained
		 * in subfolders). In the case of no matches, an empty list is returned.
		 *
		 * @throws IOException
		 *             --- in case of some I/O failure.
		 *
		 * @param ct
		 * @return
		 */
		public List<Path.Entry<?>> getAll() throws IOException;

		/**
		 * Get all objects matching a given content filter stored in this folder
		 * (including its subfolders). In the case of no matches, an empty list
		 * is returned.
		 *
		 * @throws IOException
		 *             --- in case of some I/O failure.
		 *
		 * @param ct
		 * @return
		 */
		public <T> void getAll(Content.Filter<T> ct, List<Path.Entry<T>> entries)
				throws IOException;

		/**
		 * Identify all entries matching a given content filter stored in this
		 * folder (including its subfolders). In the case of no matches, an
		 * empty set is returned.
		 *
		 * @throws IOException
		 *             --- in case of some I/O failure.
		 *
		 * @param filter
		 *            --- filter to match entries with.
		 * @return
		 */
		public <T> void getAll(Content.Filter<T> filter, Set<Path.ID> entries)
				throws IOException;

		/**
		 * Create a new entry in this folder with the given ID and content-type. This
		 * will recursively construct sub-folders as necessary.
		 *
		 * @throws IOException --- in case of some I/O failure.
		 *
		 * @param entry
		 */
		public <T> Path.Entry<T> create(Path.ID id, Content.Type<T> ct) throws IOException;


		/**
		 * Delete an entry of a given content type at a given path. If the entry doesn't
		 * exist, then nothing happens.
		 *
		 * @param id
		 * @param ct
		 * @return Flag indicating whether entry was actually removed or not.
		 * @throws IOException
		 */
		public boolean remove(ID id, Content.Type<?> ct) throws IOException;

		/**
		 * Delete all entries matching a given filter, returning the number of entries
		 * removed.
		 *
		 * @param cf
		 * @return Flag indicating whether entry was actually removed or not.
		 * @throws IOException
		 */
		public int remove(Content.Filter<?> cf) throws IOException;
	}

	/**
	 * Represents the root of a hierarchy of named entries. A instance of root
	 * may correspond to a file system directory, a Jar file, or some other
	 * abstraction representings a collection of files (e.g. eclipse's
	 * <code>IContainer</code>).
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Root {

		/**
		 * Check whether or not a given entry is contained in this root;
		 *
		 * @param entry
		 * @return
		 */
		public boolean contains(Path.Entry<?> entry) throws IOException;

		/**
		 * Check whether or not a given entry and content-type is contained in
		 * this root.
		 *
		 * @throws IOException
		 *             --- in case of some I/O failure.
		 */
		public boolean exists(ID id, Content.Type<?> ct) throws IOException;

		/**
		 * Get the entry corresponding to a given ID and content type. If no
		 * such entry exists, return null.
		 *
		 * @param id
		 *            --- id of module to lookup.
		 * @throws IOException
		 *             --- in case of some I/O failure.
		 */
		public <T> Path.Entry<T> get(ID id, Content.Type<T> ct)
				throws IOException;

		/**
		 * Get all objects matching a given content filter stored in this root.
		 * In the case of no matches, an empty list is returned.
		 *
		 * @throws IOException
		 *             --- in case of some I/O failure.
		 *
		 * @param ct
		 * @return
		 */
		public <T> List<Path.Entry<T>> get(Content.Filter<T> ct)
				throws IOException;

		/**
		 * Identify all entries matching a given content filter stored in this
		 * root. In the case of no matches, an empty set is returned.
		 *
		 * @throws IOException
		 *             --- in case of some I/O failure.
		 *
		 * @param filter
		 *            --- filter to match entries with.
		 * @return
		 */
		public <T> Set<Path.ID> match(Content.Filter<T> filter)
				throws IOException;

		/**
		 * Create an entry of a given content type at a given path. If the entry
		 * already exists, then it is just returned.
		 *
		 * @param id
		 *            --- Path.ID for the new entry
		 * @param ct
		 *            --- content type of the new entry
		 * @return
		 * @throws IOException
		 */
		public <T> Path.Entry<T> create(ID id, Content.Type<T> ct)
				throws IOException;

		/**
		 * Delete an entry of a given content type at a given path. If the entry doesn't
		 * exist, then nothing happens.
		 *
		 * @param id
		 * @param ct
		 * @return Flag indicating whether entry was actually removed or not.
		 * @throws IOException
		 */
		public boolean remove(ID id, Content.Type<?> ct) throws IOException;

		/**
		 * Delete all entries matching a given filter, returning the number of entries
		 * removed.
		 *
		 * @param cf
		 * @return Flag indicating whether entry was actually removed or not.
		 * @throws IOException
		 */
		public int remove(Content.Filter<?> cf) throws IOException;

		/**
		 * Create a relative root. That is, a root which is relative to this
		 * root.
		 *
		 * @param id
		 * @return
		 * @throws IOException
		 */
		public Path.RelativeRoot createRelativeRoot(ID id) throws IOException;

		/**
		 * Force root to flush entries to permanent storage (where appropriate).
		 * This is essential as, at any given moment, path entries may only be
		 * stored in memory. We must flush them to disk in order to preserve any
		 * changes that were made.
		 */
		public void flush() throws IOException;

		/**
		 * Force root to refresh entries from permanent storage (where
		 * appropriate). For items which has been modified, this operation has
		 * no effect (i.e. the new contents are retained).
		 */
		public void refresh() throws IOException;
	}

	public interface RelativeRoot extends Root {
		/**
		 * Get the parent root to which this is relative.
		 *
		 * @return
		 */
		Path.Root getParent();
	}

	/**
	 * A generic mechanism for selecting one or more paths. For example, one
	 * might specify an includes="whiley/**\/*.whiley" filter on a given root to
	 * identify which source files should be compiled. This would be implemented
	 * using either a content or path filter.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Filter {

		/**
		 * Check whether a given entry is matched by this filter.
		 *
		 * @param id
		 *            --- id to test.
		 * @return --- true if it matches, otherwise false.
		 */
		public boolean matches(Path.ID id);

		/**
		 * Check whether a given subpath is matched by this filter. A matching
		 * subpath does not necessarily identify an exact match; rather, it may
		 * be an enclosing folder.
		 *
		 * @param id
		 * @return
		 */
		public boolean matchesSubpath(Path.ID id);
	}

}
