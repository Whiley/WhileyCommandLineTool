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
package wycli.commands;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import wybs.lang.Build;
import wybs.util.Logger;
import wycli.cfg.Configuration;
import wycli.cfg.Configuration.Schema;
import wycli.lang.Command;
import wyfs.lang.Path;

public class Clean implements Command {
	/**
	 * The descriptor for this command.
	 */
	public static final Command.Descriptor DESCRIPTOR = new Command.Descriptor() {
		@Override
		public String getName() {
			return "clean";
		}

		@Override
		public String getDescription() {
			return "Remove all target (i.e. binary) files";
		}

		@Override
		public List<Option.Descriptor> getOptionDescriptors() {
			return Arrays.asList(Command.OPTION_FLAG("verbose", "generate verbose information", false));
		}

		@Override
		public Schema getConfigurationSchema() {
			return Configuration.EMPTY_SCHEMA;
		}

		@Override
		public List<Descriptor> getCommands() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public Command initialise(Command.Environment environment) {
			return new Clean(environment);
		}

	};

	/**
	 * The enclosing project for this build
	 */
	private final Command.Environment environment;

	/**
	 * Logger
	 */
	private Logger logger;

	public Clean(Command.Environment environment) {
		this.environment = environment;
		this.logger = environment.getLogger();
	}

	@Override
	public Descriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public void initialise() {
	}

	@Override
	public void finalise() {
		// Nothing to do here either
	}

	@Override
	public boolean execute(Template template) {
		try {
			// Extract options
			boolean verbose = template.getOptions().get("verbose", Boolean.class);
			if(project == null) {
				// Clean all projects
				for(Build.Project p : environment.getProjects()) {
					execute(p,verbose);
				}
			} else {
				// Clean selected project
				execute(project,verbose);
			}
			//
			return true;
		} catch (Exception e) {
			// FIXME: do something here??
			e.printStackTrace();
			return false;
		}
	}

	private boolean execute(Build.Project project, boolean verbose) throws IOException {
		// Identify the project root
		Path.Root root = project.getRoot();
		// Extract all registered platforms
		List<Build.Task> targets = project.getTasks();
		// Remove all intermediate files
		for (int i = 0; i != targets.size(); ++i) {
			Path.Entry<?> target = targets.get(i).getTarget();
			boolean ok = root.remove(target.id(), target.contentType());
			if (verbose && ok) {
				logger.logTimedMessage("removing  " + target.id(), 0, 0);
			} else if (verbose) {
				logger.logTimedMessage("failed removing  " + target.id(), 0, 0);
				return false;
			} else if (!ok) {
				return false;
			}
		}
		logger.logTimedMessage("cleaned " + targets.size() + " file(s)", 0, 0);
		return true;
	}

}
