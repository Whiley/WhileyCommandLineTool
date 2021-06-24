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
import java.util.*;

import wyfs.lang.Content;
import wyfs.lang.Path;

/**
 * Provides a simple implementation of <code>Path.Entry</code>. This caches
 * content in a field and employs a <code>modifies</code> bit to determine if
 * that content needs to be written to permanent storage.
 *
 * @author David J. Pearce
 *
 * @param <T>
 */
public abstract class AbstractEntry<T> implements Path.Entry<T> {
	protected final Path.ID id;
	protected Content.Type<T> contentType;
	protected T contents = null;
	protected boolean modified = false;

	public AbstractEntry(Path.ID mid) {
		this.id = mid;
	}

	@Override
	public Path.ID id() {
		return id;
	}

	@Override
	public void touch() {
		this.modified = true;
	}

	@Override
	public boolean isModified() {
		return modified;
	}

	@Override
	public Content.Type<T> contentType() {
		return contentType;
	}

	@Override
	public void refresh() throws IOException {
		if(!modified) {
			contents = null; // reset contents
		}
	}

	@Override
	public void flush() throws IOException {
		if(modified && contents != null) {
			contentType.write(outputStream(), contents);
			this.modified = false;
		}
	}

	@Override
	public T read() throws IOException {
		if (contents == null) {
			contents = contentType.read(this,inputStream());
		}
		return contents;
	}

	@Override
	public void write(T contents) throws IOException {
		this.modified = true;
		this.contents = contents;
	}

	@Override
	public void associate(Content.Type<T> contentType, T contents) {
		if(this.contentType != null) {
			throw new IllegalArgumentException("content type already associated with this entry");
		}
		this.contentType = contentType;
		this.contents = contents;
	}
}
