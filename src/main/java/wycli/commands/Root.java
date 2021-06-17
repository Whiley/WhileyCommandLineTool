package wycli.commands;

import wycli.cfg.Configuration;
import wycli.lang.Command;
import wyfs.lang.Path;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Root implements Command {
    // Done
    public static final Command.Descriptor DESCRIPTOR = new Command.Descriptor() {
        @Override
        public Configuration.Schema getConfigurationSchema() {
            return null;
        }

        @Override
        public List<Command.Option.Descriptor> getOptionDescriptors() {
            return Arrays.asList(
                    Command.OPTION_FLAG("verbose", "generate verbose information about the build", false),
                    Command.OPTION_POSITIVE_INTEGER("profile", "generate profiling information about the build", 0),
                    Command.OPTION_FLAG("brief", "generate brief output for syntax errors", false));
        }

        @Override
        public String getName() {
            return "wy";
        }

        @Override
        public String getDescription() {
            return "Command-line interface for the Whiley Compiler Collection";
        }

        @Override
        public List<Command.Descriptor> getCommands() {
            // FIXME: very broken?
            return Collections.EMPTY_LIST;
        }

        @Override
        public Command initialise(Environment parent) {
            return new Root(parent);
        }
    };

    private final Command.Environment environment;

    public Root(Command.Environment environment) {
        this.environment = environment;
    }

    @Override
    public Command.Descriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void initialise() {
    }

    @Override
    public void finalise() {
    }

    @Override
    public boolean execute(Path.ID path, Command.Template template) throws Exception {
        boolean verbose = template.getOptions().get("verbose", Boolean.class);
        //
        if (template.getChild() != null) {
            // Execute a subcommand
            template = template.getChild();
            // Access the descriptor
            Command.Descriptor descriptor = template.getCommandDescriptor();
            // Construct an instance of the command
            Command command = descriptor.initialise(environment);
            //
            return command.execute(path,template);
        } else {
//							// Initialise command
//							Command cmd = Help.DESCRIPTOR.initialise(environment);
//							// Execute command
//							return cmd.execute(path, template);
            throw new RuntimeException("should call into help command here");
        }
    }


}
