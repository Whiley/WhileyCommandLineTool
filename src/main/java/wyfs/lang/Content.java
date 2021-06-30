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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Predicate;

import wycc.lang.Filter;
import wycc.lang.Path;

public class Content {

	/**
	 * Provides an abstract mechanism for reading and writing file in
	 * a given format. Whiley source files (*.whiley) are one example, whilst JVM
	 * class files (*.class) are another.
	 *
	 * @author David J. Pearce
	 *
	 * @param <T>
	 */
	public interface Type<T> {

		/**
		 * Get the suffix associated with this content type
		 *
		 * @return
		 */
		public String getSuffix();

		/**
		 * Physically read the raw bytes from a given input stream and convert into the
		 * format described by this content type.
		 *
		 * @param id    Name of this entry.
		 * @param input Input stream representing in the format described by this
		 *              content type.
		 * @return
		 */
		public T read(Path id, InputStream input) throws IOException;

		/**
		 * Convert an object in the format described by this content type into
		 * an appropriate byte stream and write it to an output stream
		 *
		 * @param output
		 *            --- stream which this value is to be written to.
		 * @param value
		 *            --- value to be converted into bytes.
		 */
		public void write(OutputStream output, T value) throws IOException;
	}

	/**
	 * Provides a general mechanism for reading content from a given source.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Source<T> {
		/**
		 * Get a given piece of content from this source.
		 *
		 * @param <T>
		 * @param kind
		 * @param p
		 * @return
		 */
		public <S extends T> S get(Class<S> kind, Path p);

		/**
		 * Find all content matching a given filter.
		 *
		 * @param <S>
		 * @param kind
		 * @param f
		 * @return
		 */
		public <S extends T> List<S> match(Class<S> kind, Filter f);

		/**
		 * Find all content matching a given predicate.
		 *
		 * @param <S>
		 * @param kind
		 * @param f
		 * @return
		 */
		public <S extends T> List<S> match(Class<S> kind, Predicate<S> f);
	}

	/**
	 * Provides a general mechanism for writing content into a given source.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Sink<T> {
		/**
		 * Write a given piece of content into this sink,
		 *
		 * @param <T>
		 * @param kind
		 * @param p
		 * @param value
		 */
		public void put(Path p, T value);
	}

	/**
	 * <p>
	 * Responsible for associating content types to path entries. The simplest
	 * way to do this is to base the decision purely on the suffix of the entry
	 * in question. A standard implementation (wyc.util.SuffixRegistry) is
	 * provided for this common case.
	 * </p>
	 *
	 * <p>
	 * In some situations, it does occur on occasion that suffix alone is not
	 * enough. For example, a JVM class file may correspond to multiple content
	 * types if it may come from different source languages. In such cases, a
	 * probe of the content may be required to fully determine the content type.
	 * </p>
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Registry {
		/**
		 * Determine an appropriate suffix for a given content type.
		 *
		 * @param t
		 * @return
		 */
		public String suffix(Type<?> t);

		/**
		 * Determine the content type appropriate for a given suffix (if any).
		 *
		 * @param suffix
		 * @return <code>null</code> if none found.
		 */
		public Type<?> contentType(String suffix);
	}
}
