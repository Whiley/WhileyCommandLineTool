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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import wybs.io.SyntacticHeapPrinter;
import wybs.lang.SyntacticHeap;
import wybs.util.Logger;
import wybs.util.AbstractCompilationUnit.Value;
import wycli.cfg.Configuration;
import wycli.lang.Command;
import wycli.lang.Command.Descriptor;
import wycli.lang.Command.Option;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.util.Trie;

public class Inspect implements Command {

	public static final Trie INSPECT_WIDTH = Trie.fromString("inspect/width");
	public static final Trie INSPECT_INDENT = Trie.fromString("inspect/indent");

	public static final Configuration.Schema SCHEMA = Configuration
			.fromArray(Configuration.BOUND_INTEGER(INSPECT_WIDTH, "fix display width", new Value.Int(80), 0),
					Configuration.BOUND_INTEGER(INSPECT_INDENT, "indentation width (for structured view)",
							new Value.Int(3), 0));

	public static final List<Option.Descriptor> OPTIONS = Arrays
			.asList(Command.OPTION_FLAG("full", "display full output (i.e. including unreachable garbage)", false),
					Command.OPTION_FLAG("raw", "display raw output", false));

	/**
	 * The descriptor for this command.
	 */
	public static final Command.Descriptor DESCRIPTOR = new Command.Descriptor() {
		@Override
		public String getName() {
			return "inspect";
		}

		@Override
		public String getDescription() {
			return "Inspect a given project file";
		}

		@Override
		public List<Option.Descriptor> getOptionDescriptors() {
			return OPTIONS;
		}

		@Override
		public Configuration.Schema getConfigurationSchema() {
			return SCHEMA;
		}

		@Override
		public List<Descriptor> getCommands() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public Command initialise(Command.Environment environment) {
			// FIXME: should have some framework for output, rather than hard-coding
			// System.out.
			return new Inspect(System.out, environment);
		}
	};

	private final PrintStream out;
	private final Command.Environment environment;

	public Inspect(PrintStream out, Command.Environment environment) {
		this.environment = environment;
		this.out = out;
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
	}

	@Override
	public boolean execute(wybs.lang.Build.Repository repository, Template template) throws Exception {
		boolean garbage = template.getOptions().get("full", Boolean.class);
		boolean raw = template.getOptions().get("raw", Boolean.class);
		//
		int width = environment.get(Value.Int.class, INSPECT_WIDTH).unwrap().intValue();
		int indent = environment.get(Value.Int.class, INSPECT_INDENT).unwrap().intValue();

		List<String> files = template.getArguments();
		for (String file : files) {
			Content.Type<?> ct = getContentType(file);
			wybs.lang.Build.Entry entry = getEntry(repository, file, ct);
			if(entry == null) {
				out.println("unknown file: " + file);
			} else if(!raw && ct instanceof Content.Printable<?>){
				Content.Printable cp = (Content.Printable<?>) ct;
				cp.print(out, entry);
			} else {
				inspect(entry, ct, garbage, width);
			}
		}
		return true;
	}

	/**
	 * Determine the content type for this file.
	 *
	 * @param file
	 * @return
	 */
	private Content.Type<?> getContentType(String file) {
		String[] parts = file.split("\\.");
		// Attempt to identify content type
		Content.Type<?> ct = environment.getContentRegistry().contentType(parts[parts.length - 1]);
		// Default is just a binary file
		return ct != null ? ct : Content.BinaryFile;
	}

	/**
	 * Get the entry associated with this file.
	 *
	 * @param file
	 * @param ct
	 * @return
	 * @throws IOException
	 */
	public wybs.lang.Build.Entry getEntry(wybs.lang.Build.Repository<?> repository, String file, Content.Type ct) throws IOException {
		// Strip suffix
		file = file.replace("." + ct.getSuffix(), "");
		// Determine path id
		Path.ID id = Trie.fromString(file);
		// Get the file from the repository root
		return repository.get().get(ct, id);
	}

	/**
	 * Inspect a given path entry.
	 *
	 * @param entry
	 * @param ct
	 * @throws IOException
	 */
	private void inspect(wybs.lang.Build.Entry entry, Content.Type<?> ct, boolean garbage, int width) throws IOException {
		if (entry instanceof SyntacticHeap) {
			new SyntacticHeapPrinter(new PrintWriter(out), garbage).print((SyntacticHeap) entry);
		} else {
			throw new IllegalArgumentException("internal failure");
			//inspectBinaryFile(readAllBytes(entry.inputStream()),width);
		}
	}

	/**
	 * Inspect a given binary file. That is a file for which we don't have a better
	 * inspector.
	 *
	 * @param bytes
	 */
	private void inspectBinaryFile(byte[] bytes, int width) {
		for (int i = 0; i < bytes.length; i += width) {
			out.print(String.format("0x%04X ", i));
			// Print out databytes
			for (int j = 0; j < width; ++j) {
				if(j+i < bytes.length) {
					out.print(String.format("%02X ", bytes[i+j]));
				} else {
					out.print("   ");
				}
			}
			//
			for (int j = 0; j < width; ++j) {
				if(j+i < bytes.length) {
					char c = (char) bytes[i+j];
					if(c >= 32 && c < 128) {
						out.print(c);
					} else {
						out.print(".");
					}
				}
			}
			//
			out.println();
		}
	}

	private static byte[] readAllBytes(InputStream in) throws IOException {
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
}

