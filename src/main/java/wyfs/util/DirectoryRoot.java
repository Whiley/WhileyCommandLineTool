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

import java.io.*;
import java.util.*;

import wycc.lang.Path;
import wyfs.lang.Content;
import wyfs.lang.FileSystem;

/**
 * Provides an implementation of <code>Path.Root</code> for representing a file
 * system directory.
 *
 * @author David J. Pearce
 *
 */
public class DirectoryRoot extends AbstractRoot<DirectoryRoot.Folder> {

	public final static FileFilter NULL_FILTER = new FileFilter() {
		@Override
		public boolean accept(File file) {
			return true;
		}
	};

	private final FileFilter filter;
	private final File dir;

	/**
	 * Construct a directory root from a filesystem path expressed as a string,
	 * and an appropriate file filter. In converting the path to a File object,
	 * an IOException may arise if it is an invalid path.
	 *
	 * @param path
	 *            --- location of directory on filesystem, expressed as a native
	 *            path (i.e. separated using File.separatorChar, etc)
	 * @throws IOException
	 */
	public DirectoryRoot(String path, Content.Registry contentTypes) throws IOException {
		super(contentTypes);
		this.dir = new File(path);
		this.filter = NULL_FILTER;
	}

	/**
	 * Construct a directory root from a filesystem path expressed as a string,
	 * and an appropriate file filter. In converting the path to a File object,
	 * an IOException may arise if it is an invalid path.
	 *
	 * @param path
	 *            --- location of directory on filesystem, expressed as a native
	 *            path (i.e. separated using File.separatorChar, etc)
	 * @param filter
	 *            --- filter on which files are included.
	 * @throws IOException
	 */
	public DirectoryRoot(String path, FileFilter filter, Content.Registry contentTypes) throws IOException {
		super(contentTypes);
		this.dir = new File(path);
		this.filter = filter;
	}

	/**
	 * Construct a directory root from a filesystem path expressed as a string,
	 * and an appropriate file filter. In converting the path to a File object,
	 * an IOException may arise if it is an invalid path.
	 *
	 * @param path
	 *            --- location of directory on filesystem, expressed as a native
	 *            path (i.e. separated using File.separatorChar, etc)
	 * @throws IOException
	 */
	public DirectoryRoot(File dir, Content.Registry contentTypes) throws IOException {
		super(contentTypes);
		this.dir = dir;
		this.filter = NULL_FILTER;
	}

	/**
	 * Construct a directory root from a filesystem path expressed as a string,
	 * and an appropriate file filter. In converting the path to a File object,
	 * an IOException may arise if it is an invalid path.
	 *
	 * @param path
	 *            --- location of directory on filesystem, expressed as a native
	 *            path (i.e. separated using File.separatorChar, etc)
	 * @param filter
	 *            --- filter on which files are included.
	 * @throws IOException
	 */
	public DirectoryRoot(File dir, FileFilter filter, Content.Registry contentTypes) throws IOException {
		super(contentTypes);
		this.dir = dir;
		this.filter = filter;
	}

	public File location() {
		return dir;
	}

	@Override
	public String toString() {
		return dir.getPath();
	}

	@Override
	protected Folder root() {
		return new Folder(Path.ROOT);
	}

	/**
	 * Given a list of physical files on the file system, determine their
	 * corresponding <code>Path.Entry</code> instances in this root (if there
	 * are any).
	 *
	 * @param files
	 *            --- list of files on the physical file system.
	 * @param contentType
	 *            --- content type of files to match.
	 * @return --- list of path entries where each entry matches the
	 *         corresponding entry in files, or is null (if there is no match).
	 * @throws IOException
	 */
	public <T> List<FileSystem.Entry<T>> find(List<File> files,
											  Content.Type<T> contentType)
			throws IOException {
		ArrayList<FileSystem.Entry<T>> sources = new ArrayList<>();
		String suffix = "." + contentTypes.suffix(contentType);
		String location = location().getCanonicalPath();

		for (File file : files) {
			String filePath = file.getCanonicalPath();
			if (filePath.startsWith(location)) {
				int end = location.length();
				if (end > 1) {
					end++;
				}
				String module = filePath.substring(end).replace(
						File.separatorChar, '/');
				if (module.endsWith(suffix)) {
					module = module.substring(0,
							module.length() - suffix.length());
					Path mid = Path.fromString(module);
					FileSystem.Entry<T> entry = this.get(mid, contentType);
					if (entry != null) {
						sources.add(entry);
						continue;
					}
				}
			}
		}

		return sources;
	}

	/**
	 * An entry is a file on the file system which represents a Whiley module. The
	 * file may be encoded in a range of different formats. For example, it may be a
	 * source file and/or a binary wyil file.
	 *
	 * @author David J. Pearce
	 *
	 */
	public static final class Entry<T> extends AbstractEntry<T> implements FileSystem.Entry<T> {
		private final java.io.File file;

		public Entry(Path id, java.io.File file) {
			super(id);
			this.file = file;
		}

		@Override
		public String location() {
			return file.getPath();
		}

		@Override
		public long lastModified() {
			return file.lastModified();
		}

		public File file() {
			return file;
		}

		@Override
		public String suffix() {
			String filename = file.getName();
			String suffix = "";
			int pos = filename.lastIndexOf('.');
			if (pos > 0) {
				suffix = filename.substring(pos + 1);
			}
			return suffix;
		}

		@Override
		public InputStream inputStream() throws IOException {
			return new FileInputStream(file);
		}

		@Override
		public OutputStream outputStream() throws IOException {
			file.getParentFile().mkdirs();
			return new FileOutputStream(file);
		}

		@Override
		public String toString() {
			return file.toString();
		}
	}

	/**
	 * Represents a directory on a physical file system.
	 *
	 * @author David J. Pearce
	 *
	 */
	public final class Folder extends AbstractFolder {
		public Folder(Path id) {
			super(id);
		}

		@Override
		protected FileSystem.Item[] contents() throws IOException {
			File myDir = new File(dir, id.toString().replace('/', File.separatorChar));

			if (myDir.exists() && myDir.isDirectory()) {
				File[] files = myDir.listFiles(filter);
				FileSystem.Item[] items = new FileSystem.Item[files.length];
				int count = 0;
				for(int i=0;i!=files.length;++i) {
					File file = files[i];
					String filename = file.getName();
					if (file.isDirectory()) {
						items[count++] = new Folder(id.append(filename));
					} else {
						int idx = filename.lastIndexOf('.');
						if (idx > 0) {
							String name = filename.substring(0, idx);
							Path oid = id.append(name);
							Entry e = new Entry(oid, file);
							items[count++] = e;
						}
					}
				}

				if(count != items.length) {
					// trim the end since we didn't use all allocated elements.
					return Arrays.copyOf(items,count);
				} else {
					// minor optimisation
					return items;
				}
			} else {
				return new FileSystem.Item[0];
			}
		}

		@Override
		public <T> FileSystem.Entry<T> create(Path nid, Content.Type<T> ct)
				throws IOException {
			if (nid.size() == id.size() + 1) {
				// attempting to create an entry in this folder
				FileSystem.Entry<T> e = super.get(nid.subpath(0, 1), ct);
				if (e == null) {
					// Entry doesn't already exist, so create it
					String physID = nid.toString().replace('/',
							File.separatorChar);
					physID = physID + "." + contentTypes.suffix(ct);
					File nfile = new File(dir.getAbsolutePath()
							+ File.separatorChar + physID);
					e = new Entry(nid, nfile);
					e.associate(ct, null);
					super.insert(e);
				}
				return e;
			} else {
				// attempting to create entry in subfolder.
				String folderName = nid.get(id.size());
				FileSystem.Folder folder = getFolder(folderName);
				if (folder == null) {
					// Folder doesn't already exist, so create it.
					folder = new Folder(id.append(folderName));
					super.insert(folder);
				}
				return folder.create(nid, ct);
			}
		}

		@Override
		public boolean remove(Path id, Content.Type<?> type) throws IOException {
			FileSystem.Entry<?> entry = get(id, type);
			//
			if (entry != null) {
				DirectoryRoot.Entry<?> e = (Entry<?>) entry;
				super.remove(id, type);
				// Physically delete underlying file
				return e.file().delete();
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return dir + ":" + id;
		}
	}

}
