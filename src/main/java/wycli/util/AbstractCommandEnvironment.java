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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import wybs.util.Logger;
import wycli.cfg.Configuration;
import wycli.cfg.Configuration.Schema;
import wycli.lang.Command;
import wyfs.lang.Content;
import wyfs.lang.Content.Registry;
import wyfs.lang.Content.Type;
import wyfs.lang.Path.Entry;
import wyfs.lang.Path.Filter;
import wyfs.lang.Path.ID;
import wyfs.lang.Path.Root;

public abstract class AbstractCommandEnvironment implements Command.Environment {
	/**
	 * Complete configuration
	 */
	protected Configuration configuration;

	/**
	 * List of all known content types to the system.
	 */
	protected ArrayList<Content.Type<?>> contentTypes = new ArrayList<>();

	/**
	 * List of all known commands registered by plugins.
	 */
	protected ArrayList<Command.Descriptor> commandDescriptors = new ArrayList<>();

	/**
	 * List of all known build platforms registered by plugins.
	 */
	protected ArrayList<Command.Platform> buildPlatforms = new ArrayList<>();

	/**
	 * Standard log output
	 */
	protected Logger logger;

	/**
	 * Top-level executor used for compiling all projects within this environment.
	 */
	protected ExecutorService executor;


	public AbstractCommandEnvironment(Configuration configuration, Logger logger, ExecutorService executor) {
		this.configuration = configuration;
		this.logger = logger;
		this.executor = executor;
	}

	/**
	 * The master registry which provides knowledge of all file types used within
	 * the system.
	 */
	protected final Content.Registry registry = new Content.Registry() {

		@Override
		public String suffix(Type<?> t) {
			return t.getSuffix();
		}

		@Override
		public void associate(Entry<?> e) {
			for (Content.Type<?> ct : contentTypes) {
				if (ct.getSuffix().equals(e.suffix())) {
					e.associate((Content.Type) ct, null);
					return;
				}
			}
			e.associate((Content.Type) Content.BinaryFile, null);
		}


		@Override
		public Content.Type<?> contentType(String suffix) {
			for (Content.Type<?> ct : contentTypes) {
				if (ct.getSuffix().equals(suffix)) {
					return ct;
				}
			}
			return null;
		}
	};


	@Override
	public Registry getContentRegistry() {
		return registry;
	}

	public List<Type<?>> getContentTypes() {
		return contentTypes;
	}

	@Override
	public List<Command.Descriptor> getCommandDescriptors() {
		return commandDescriptors;
	}

	/**
	 * Get the list of available build platforms. These help determine what the
	 * valid build targets are.
	 *
	 * @return
	 */
	@Override
	public List<Command.Platform> getBuildPlatforms() {
		return buildPlatforms;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	@Override
	public Schema getConfigurationSchema() {
		return configuration.getConfigurationSchema();
	}

	@Override
	public <T> boolean hasKey(ID key) {
		return configuration.hasKey(key);
	}

	@Override
	public <T> T get(Class<T> kind, ID key) {
		return configuration.get(kind, key);
	}

	@Override
	public <T> void write(ID key, T value) {
		configuration.write(key, value);
	}

	@Override
	public List<ID> matchAll(Filter filter) {
		return configuration.matchAll(filter);
	}
}
