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
package wycli.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import wycc.util.AbstractCompilationUnit.Value.UTF8;
import wycli.Schemas;
import wycli.cfg.ConfigFile;
import wycli.cfg.Configuration;
import wycli.lang.Command;
import wycli.lang.Package;
import wycli.lang.Semantic;
import wycli.lang.Package.Repository;
import wycc.util.Pair;
import wycc.lang.Path;
import wyfs.lang.FileSystem;

/**
 * Provides a default and relatively simplistic approach for resolving packages.
 *
 * @author David J. Pearce
 *
 */
public class StdPackageResolver implements Package.Resolver {
	private final Command.Environment environment;
	private final Package.Repository repository;

	public StdPackageResolver(Command.Environment environment, Package.Repository repository) {
		this.repository = repository;
		this.environment = environment;
	}

	@Override
	public List<FileSystem.Root> resolve(Configuration cf) throws IOException {
		ArrayList<FileSystem.Root> packages = new ArrayList<>();
		// Extract all dependencies from target config file
		List<Pair<String,String>> dependencies = extractDependencies(cf);
		// Visited set stores all packages we have visited. This is used to ensure no
		// package is visited more than once.
		HashSet<Pair<String,String>> visited = new HashSet<>(dependencies);
		// Iterate until no more dependencies to resolve
		while(dependencies.size() > 0) {
			// Iterate current batch of dependencies
			dependencies = process(packages,dependencies,visited);
		}
		return packages;
	}

	@Override
	public Repository getRepository() {
		return repository;
	}

	private List<Pair<String, String>> process(List<FileSystem.Root> packages, List<Pair<String, String>> batch, Set<Pair<String, String>> visited) throws IOException {
		// Children will store all dependencies of those in batch
		ArrayList<Pair<String,String>> children = new ArrayList<>();
		// Process current batch of dependencies
		for (Pair<String, String> dep : batch) {
			String name = dep.first();
			Semantic.Version version = resolveLatestCompatible(name,new Semantic.Version(dep.second()));
			FileSystem.Root pkg = repository.get(name, version);
			if (pkg != null) {
				// Read package configuration file.
				FileSystem.Entry<ConfigFile> entry = pkg.get(Path.fromString("wy"), ConfigFile.ContentType);
				if (entry == null) {
					// Something is wrong
					environment.getLogger()
							.logTimedMessage("Corrupt package " + pkg + "-v" + version + " (missing wy.toml)", 0, 0);
				} else {
					// Convert file into configuration
					Configuration cf = entry.read().toConfiguration(Schemas.PACKAGE_SCHEMA, false);
					// Add all (non-visited) child dependencies
					for(Pair<String, String> d : extractDependencies(cf)) {
						if(!visited.contains(d)) {
							visited.add(d);
							children.add(d);
						}
					}
					// Done
					packages.add(pkg);
					// Log event
					environment.getLogger().logTimedMessage("Loaded " + name + "-v" + version, 0, 0);
				}
			}
		}
		//
		return children;
	}

	/**
	 * For a given package and version, determine the latest compatible version
	 * whilst respecting the rules of semantic versioning. Thus, for a given major
	 * version, the latest minor and micro versions are desired.
	 *
	 * @param pkg
	 * @param version
	 * @return
	 * @throws IOException
	 */
	private Semantic.Version resolveLatestCompatible(String pkg, Semantic.Version version) throws IOException {
		// list all possible versions of the given package
		Set<Semantic.Version> versions = repository.list(pkg);
		//
		Semantic.Version latest = version;
		//
		for (Semantic.Version v : versions) {
			if (v.getMajor() == version.getMajor() && v.compareTo(latest) > 0) {
				latest = v;
			}
		}
		//
		return latest;
	}

	private List<Pair<String, String>> extractDependencies(Configuration cf) {
		//
		List<Path> deps = cf.matchAll(Path.fromFilterString("dependencies/**"));
		// Determine dependency roots
		List<Pair<String, String>> pairs = new ArrayList<>();
		for (int i = 0; i != deps.size(); ++i) {
			Path dep = deps.get(i);
			// Get dependency name
			String name = dep.get(1);
			// Get version string
			UTF8 version = cf.get(UTF8.class, dep);
			//
			pairs.add(new Pair<>(name, version.toString()));
		}
		return pairs;
	}
}
