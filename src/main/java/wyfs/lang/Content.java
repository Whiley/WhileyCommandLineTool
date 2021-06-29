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
	 * Provide a simple mechanism for printing content to an output stream. This is
	 * mostly useful for debugging.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Printable<T> extends Type<T> {
		/**
		 * Print this content type to a given input stream.
		 *
		 * @param output
		 * @throws IOException
		 */
		public void print(PrintStream output, T content) throws IOException;
	}

	/**
	 * Represents a generic binary file of unknown content. This is useful for
	 * associating anything which the given content registry does not recognise.
	 */
	public static Content.Type<byte[]> BinaryFile = new Content.Type<byte[]>() {

		@Override
		public String getSuffix() {
			return "(bin)";
		}

		@Override
		public void write(OutputStream output, byte[] bytes) throws IOException {
			output.write(bytes);
		}

		@Override
		public byte[] read(Path id, InputStream input) throws IOException {
			throw new UnsupportedOperationException();
		}

	};

	/**
	 * A generic mechanism for selecting a subset of content based on a path
	 * filter and a content type. For example, one might specify an
	 * includes="whiley/**\/*.whiley" filter on a given root to identify which
	 * source files should be compiled. This would be implemented using either a
	 * content or path filter.
	 *
	 * @author David J. Pearce
	 *
	 * @param <T>
	 */
	public interface Filter<T> {

		/**
		 * Check whether a given entry is matched by this filter.
		 *
		 * @param entry
		 *            --- entry to test.
		 * @return --- entry (retyped) if it matches, otherwise null.
		 */
		public boolean matches(Path id, Content.Type<T> ct);

		/**
		 * Check whether a given subpath is matched by this filter. A matching
		 * subpath does not necessarily identify an exact match; rather, it may
		 * be an enclosing folder.
		 *
		 * @param id
		 * @return
		 */
		public boolean matchesSubpath(Path id);
	}

	/**
	 * Construct a content filter from a path filter and a content type.
	 *
	 * @param filter --- path filter
	 * @param contentType
	 * @return
	 */
	public static <T> Filter<T> filter(final wycc.lang.Filter filter, final Content.Type<T> contentType) {
		return new Filter<T>() {
			@Override
			public boolean matches(Path id, Content.Type<T> ct) {
				return ct == contentType && filter.matches(id);
			}
			@Override
			public boolean matchesSubpath(Path id) {
				return filter.matches(id);
			}
			@Override
			public String toString() {
				return filter.toString();
			}
		};
	}

	/**
	 * Construct a content filter from a string representing a path filter and a content type.
	 *
	 * @param filter --- path filter
	 * @param contentType
	 * @return
	 */
	public static <T> Filter<T> filter(final String pathFilter, final Content.Type<T> contentType) {
		final wycc.lang.Filter filter = wycc.lang.Filter.fromString(pathFilter);
		return new Filter<T>() {
			@Override
			public boolean matches(Path id, Content.Type<T> ct) {
				return ct == contentType && filter.matches(id);
			}
			@Override
			public boolean matchesSubpath(Path id) {
				return filter.matches(id);
			}
			@Override
			public String toString() {
				return filter.toString();
			}
		};
	}
	/**
	 * Combine two filters together produce one filter whose items must be
	 * matched by at least one of the original filters.
	 *
	 * @param f1
	 * @param f2
	 * @return
	 */
	public static <T> Filter<T> or(final Filter<T> f1, final Filter<T> f2) {
		return new Filter<T>() {
			@Override
			public boolean matches(Path id, Content.Type<T> ct) {
				return f1.matches(id, ct) || f2.matches(id, ct);
			}
			@Override
			public boolean matchesSubpath(Path id) {
				return f1.matchesSubpath(id) || f2.matchesSubpath(id);
			}
			@Override
			public String toString() {
				return f1.toString() + "|" + f2.toString();
			}
		};
	}

	/**
	 * Combine two filters together produce one filter whose items must be
	 * matched by both of the original filters.
	 *
	 * @param f1
	 * @param f2
	 * @return
	 */
	public static <T> Filter<T> and(final Filter<T> f1, final Filter<T> f2) {
		return new Filter<T>() {
			@Override
			public boolean matches(Path id, Content.Type<T> ct) {
				return f1.matches(id, ct) && f2.matches(id, ct);
			}
			@Override
			public boolean matchesSubpath(Path id) {
				return f1.matchesSubpath(id) && f2.matchesSubpath(id);
			}
			@Override
			public String toString() {
				return f1.toString() + "&" + f2.toString();
			}
		};
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
