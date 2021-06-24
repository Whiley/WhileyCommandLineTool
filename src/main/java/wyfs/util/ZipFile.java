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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import wyfs.lang.Content;
import wyfs.lang.Path;

/**
 * A shim for handling ZipFiles in a uniform fashion within the Whiley File
 * System (WyFS).
 *
 * @author David J. Pearce
 *
 */
public class ZipFile {

	public static Content.Type<ZipFile> ContentType = new Content.Type<ZipFile>() {

		@Override
		public String getSuffix() {
			return "zip";
		}

		@Override
		public ZipFile read(Path.Entry<ZipFile> e, InputStream input) throws IOException {
			return new ZipFile(e.inputStream());
		}

		@Override
		public ZipFile read(Path.ID id, InputStream input) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void write(OutputStream output, ZipFile zf) throws IOException {
			ZipOutputStream zout = new ZipOutputStream(output);
			for (int i = 0; i != zf.size(); ++i) {
				Entry e = zf.get(i);
				zout.putNextEntry(e.entry);
				zout.write(e.bytes);
				zout.closeEntry();
			}
			zout.finish();
		}

	};

	/**
	 * Contains the list of entries in the zip file.
	 */
	private final List<Entry> entries;

	/**
	 * Construct an empty ZipFile
	 */
	public ZipFile() {
		this.entries = new ArrayList<>();
	}

	/**
	 * Construct a ZipFile from a given input stream representing a zip file.
	 *
	 * @param input
	 */
	public ZipFile(InputStream input) throws IOException {
		this.entries = new ArrayList<>();
		// Read all entries from the input stream
		ZipInputStream zin = new ZipInputStream(input);
		ZipEntry e;
		while ((e = zin.getNextEntry()) != null) {
			byte[] contents = readEntryContents(zin);
			entries.add(new Entry(e, contents));
			zin.closeEntry();
		}
		zin.close();
	}

	public int size() {
		return entries.size();
	}

	public void add(ZipEntry entry, byte[] bytes) {
		this.entries.add(new Entry(entry,bytes));
	}

	public Entry get(int i) {
		return entries.get(i);
	}

	private byte[] readEntryContents(InputStream in) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		// Read bytes in max 1024 chunks
		byte[] data = new byte[1024];
		// Read all bytes from the input stream
		while ((nRead = in.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		// Done
		buffer.flush();
		return buffer.toByteArray();
	}

	public static final class Entry {
		public final ZipEntry entry;
		public final byte[] bytes;

		public Entry(ZipEntry entry, byte[] bytes) {
			this.entry = entry;
			this.bytes = bytes;
		}

		public String getName() {
			return entry.getName();
		}

		public boolean isDirectory() {
			return entry.isDirectory();
		}

		public long getTime() {
			return entry.getTime();
		}

		public InputStream getInputStream() {
			return new ByteArrayInputStream(bytes);
		}
	}
}
