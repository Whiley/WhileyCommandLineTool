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
package wycli.lang;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import wybs.lang.Build;
import wybs.util.AbstractCompilationUnit.Value;
import wycli.cfg.ConfigFile;
import wycli.cfg.Configuration;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.util.Pair;
import wyfs.util.Trie;
import wyfs.util.ZipFile;

public interface Package {

	/**
	 * This determines what files are included in a package be default (i.e. when
	 * the build/includes attribute is not specified).
	 */
	public static final Value.Array DEFAULT_BUILD_INCLUDES = new Value.Array(
			// Include package description by default
			new Value.UTF8("wy.toml"),
			// Include all wyil files by default
			new Value.UTF8("**/*.wyil"),
			// Include all whiley files by default
			new Value.UTF8("**/*.whiley")
		);

	/**
	 * Schema for packages (i.e. which applies to a single project for a given user).
	 */
	public static Configuration.Schema SCHEMA = Configuration.fromArray(
			// Required items
			Configuration.UNBOUND_STRING(Trie.fromString("package/name"), "Name of this package", new Value.UTF8("main")),
			Configuration.UNBOUND_STRING_ARRAY(Trie.fromString("package/authors"), "Author(s) of this package", false),
			Configuration.UNBOUND_STRING(Trie.fromString("package/version"), "Semantic version of this package", false),
			// Build items
			Configuration.UNBOUND_STRING_ARRAY(Trie.fromString("build/platforms"),
					"Target platforms for this package (default just \"whiley\")",
					new Value.Array(new Value.UTF8("whiley"))),
			Configuration.UNBOUND_STRING_ARRAY(Trie.fromString("build/includes"), "Files to include in package",
					DEFAULT_BUILD_INCLUDES),
			Configuration.UNBOUND_STRING(Trie.fromString("build/main"), "Identify main method", false),
			// Optional items
			Configuration.REGEX_STRING(Trie.fromString("dependencies/*"), "Packages this package depends on", false,
					Pattern.compile("\\d+.\\d+.\\d+"))
	);

	/**
	 * Responsible for resolving version strings into concrete packages.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Resolver {
		/**
		 * Resolve a given package name and version string, which may include additional
		 * modifiers (e.g. semantic versioning constraints).
		 *
		 * @param pkg
		 * @param version
		 * @return
		 */
		List<Path.Root> resolve(Configuration cf) throws IOException;

		/**
		 * Get the root repository associated with this package resolver.
		 *
		 * @return
		 */
		Repository getRepository();
	}

	/**
	 * Represents a store of packages, such as on disk or in the cloud.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Repository {
		/**
		 * Get the parent repository for this repository (or <code>null</code> if this
		 * is the root repository).
		 *
		 * @return
		 */
		public Package.Repository getParent();

		/**
		 * List all known versions of a given package. This is used for resolution,
		 * amongst other things.
		 *
		 * @param pkg
		 * @return
		 */
		public Set<Semantic.Version> list(String pkg) throws IOException;

		/**
		 * Get a given package in this repository. If no such package exists, an
		 * <code>IllegalArgumentException</code> is thrown.
		 *
		 * @param name
		 * @param version
		 * @return
		 */
		public Path.Root get(String name, Semantic.Version version) throws IOException;

		/**
		 * Put a given package into this repository.
		 *
		 * @param pkg
		 */
		public void put(ZipFile pkg, String name, Semantic.Version version) throws IOException;
	}
}
