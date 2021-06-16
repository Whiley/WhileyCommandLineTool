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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import wybs.lang.SyntacticHeap;
import wybs.lang.SyntacticItem;
import wybs.lang.SourceFile;
import wybs.lang.Build.Repository;
import wybs.util.AbstractCompilationUnit;
import wybs.util.AbstractCompilationUnit.Attribute;
import wybs.util.AbstractCompilationUnit.Attribute.Span;
import wycli.cfg.Configuration;
import wycli.cfg.Configuration.Schema;
import wycli.lang.Command;
import wyfs.lang.Path;
import wyfs.util.Pair;

public class Build implements Command {
	/**
	 * The descriptor for this command.
	 */
	public static final Command.Descriptor DESCRIPTOR = new Command.Descriptor() {
		@Override
		public String getName() {
			return "build";
		}

		@Override
		public String getDescription() {
			return "Perform build operations on an existing project";
		}

		@Override
		public List<Option.Descriptor> getOptionDescriptors() {
			return Arrays.asList(Command.OPTION_FLAG("verbose", "generate verbose information about the build", false),
					Command.OPTION_FLAG("brief", "generate brief output for syntax errors", false));
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
			return new Build(environment, System.out, System.err);
		}

	};

	/**
	 * Provides a generic place to which normal output should be directed. This
	 * should eventually be replaced.
	 */
	private final PrintStream sysout;

	/**
	 * Provides a generic place to which error output should be directed. This
	 * should eventually be replaced.
	 */
	private final PrintStream syserr;

	/**
	 * Signals that brief error reporting should be used. This is primarily used to
	 * help integration with external tools. More specifically, brief output is
	 * structured so as to be machine readable.
	 */
	protected boolean brief = false;

	/**
	 * The enclosing project for this build
	 */
	private final Command.Environment environment;

	public Build(Command.Environment environment, OutputStream sysout, OutputStream syserr) {
		this.environment = environment;
		this.sysout = new PrintStream(sysout);
		this.syserr = new PrintStream(syserr);
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
	public boolean execute(Template template) throws Exception {
		Repository repository = environment.getRepository();
		List<wybs.lang.Build.Task> tasks = new ArrayList<>();
		// Construct tasks
		for(Command.Platform p : environment.getBuildPlatforms()) {
			tasks.add(p.initialise(environment));
		}
		// Construct pipeline
		Pipeline pipeline = new Pipeline(tasks);
		// Runs tasks
		repository.apply(pipeline);
		// Look for error messages
//		for (wybs.lang.Build.Task task : project.getTasks()) {
//			printSyntacticMarkers(syserr, task.getSources(), task.getTarget());
//		}
		// Success if all pipeline stages completed
		return (pipeline.completed == tasks.size());
	}

	private static class Pipeline<S extends wybs.lang.Build.State<S>> implements Function<S,S> {
		private final List<wybs.lang.Build.Task<S>> tasks;
		private int completed;

		private Pipeline(List<wybs.lang.Build.Task<S>> tasks) {
			this.tasks = tasks;
		}

		@Override
		public S apply(S s) {
			for (int i = 0; i != tasks.size(); ++i) {
				wybs.lang.Build.Task<S> ith = tasks.get(i);
				Pair<S, Boolean> p = ith.apply(s);
				s = p.first();
				if (!p.second()) {
					// Print error messages
					break;
				}
				completed = completed + 1;
			}
			return s;
		}
	}

	/**
	 * Print out syntactic markers for all entries in the build graph. This requires
	 * going through all entries, extracting the markers and then printing them.
	 *
	 * @param executor
	 * @throws IOException
	 */
	public static void printSyntacticMarkers(PrintStream output, Collection<Path.Entry<?>> sources, Path.Entry<?> target) throws IOException {
		// Extract all syntactic markers from entries in the build graph
		List<SyntacticItem.Marker> items = extractSyntacticMarkers(target);
		// For each marker, print out error messages appropriately
		for (int i = 0; i != items.size(); ++i) {
			// Log the error message
			printSyntacticMarkers(output, sources, items.get(i));
		}
	}

	public static void printSyntacticMarkers(PrintStream output, SyntacticHeap target, SourceFile... sources) throws IOException {
		// Extract all syntactic markers from entries in the build graph
		List<SyntacticItem.Marker> items = extractSyntacticMarkers(target);
		// For each marker, print out error messages appropriately
		for (int i = 0; i != items.size(); ++i) {
			// Log the error message
			printSyntacticMarkers(output, items.get(i), sources);
		}
	}

	/**
	 * Print out an individual syntactic markers.
	 *
	 * @param marker
	 */
	public static void printSyntacticMarkers(PrintStream output, Collection<Path.Entry<?>> sources, SyntacticItem.Marker marker) {
		//
		Path.Entry<?> source = getSourceEntry(sources,marker.getSource());
		//
		Span span = marker.getTarget().getAncestor(AbstractCompilationUnit.Attribute.Span.class);
		// Read the enclosing line so we can print it
		EnclosingLine line = readEnclosingLine(source, span);
		// Sanity check we found it
		if(line != null) {
			// print the error message
			output.println(source.location() + ":" + line.lineNumber + ": " + marker.getMessage());
			// Finally print the line highlight
			printLineHighlight(output, line);
		} else {
			output.println(source.location() + ":?: " + marker.getMessage());
		}
	}

	public static void printSyntacticMarkers(PrintStream output, SyntacticItem.Marker marker, SourceFile... sources) {
		// Identify enclosing source file
		SourceFile source = getSourceEntry(marker.getSource(), sources);
		String filename = source.getID() + "." + source.getContentType().getSuffix();
		//
		Span span = marker.getTarget().getAncestor(AbstractCompilationUnit.Attribute.Span.class);
		// Read the enclosing line so we can print it
		SourceFile.Line line = source.getEnclosingLine(span.getStart().get().intValue());
		// Sanity check we found it
		if (line != null) {
			// print the error message
			output.println(filename + ":" + line.getNumber() + ": " + marker.getMessage());
			// Finally print the line highlight
			printLineHighlight(output, span, line);
		} else {
			output.println(filename + ":?: " + marker.getMessage());
		}
	}

	public static List<SyntacticItem.Marker> extractSyntacticMarkers(Path.Entry<?>... binaries) throws IOException {
		List<SyntacticItem.Marker> annotated = new ArrayList<>();
		//
		for (Path.Entry<?> binary : binaries) {
			Object o = binary.read();
			// If the object in question can be decoded as a syntactic heap then we can look
			// for syntactic messages.
			if (o instanceof SyntacticHeap) {
				annotated.addAll(extractSyntacticMarkers((SyntacticHeap) o));
			}
		}
		//
		return annotated;
	}

	/**
	 * Traverse the various binaries which have been generated looking for error
	 * messages.
	 *
	 * @param binaries
	 * @return
	 * @throws IOException
	 */
	public static List<SyntacticItem.Marker> extractSyntacticMarkers(SyntacticHeap h) throws IOException {
		List<SyntacticItem.Marker> annotated = new ArrayList<>();
		// FIXME: this just reports all syntactic markers
		annotated.addAll(h.findAll(SyntacticItem.Marker.class));
		//
		return annotated;
	}

	private static Path.Entry<?> getSourceEntry(Collection<Path.Entry<?>> sources, Path.ID id) {
		String str = id.toString();
		//
		for (Path.Entry<?> s : sources) {
			// FIXME: this is obviously a bad hack for now
			String sid = s.id().toString();
			if (sid.endsWith(str)) {
				return s;
			}
		}
		return null;
	}

	private static SourceFile getSourceEntry(Path.ID id, SourceFile... sources) {
		//
		for (SourceFile s : sources) {
			if (id.equals(s.getID())) {
				return s;
			}
		}
		return null;
	}


	private static void printLineHighlight(PrintStream output,
			EnclosingLine enclosing) {
		// NOTE: in the following lines I don't print characters
		// individually. The reason for this is that it messes up the
		// ANT task output.
		String str = enclosing.lineText;

		if (str.length() > 0 && str.charAt(str.length() - 1) == '\n') {
			output.print(str);
		} else {
			// this must be the very last line of output and, in this
			// particular case, there is no new-line character provided.
			// Therefore, we need to provide one ourselves!
			output.println(str);
		}
		str = "";
		for (int i = 0; i < enclosing.columnStart(); ++i) {
			if (enclosing.lineText.charAt(i) == '\t') {
				str += "\t";
			} else {
				str += " ";
			}
		}
		for (int i = enclosing.columnStart(); i <= enclosing.columnEnd(); ++i) {
			str += "^";
		}
		output.println(str);
	}

	private static void printLineHighlight(PrintStream output,
										   Span span,
										   SourceFile.Line enclosing) {
		// Extract line text
		String text = enclosing.getText();
		// Determine start and end of span
		int start = span.getStart().get().intValue() - enclosing.getOffset();
		int end = Math.min(text.length() - 1, span.getEnd().get().intValue() - enclosing.getOffset());
		// NOTE: in the following lines I don't print characters
		// individually. The reason for this is that it messes up the
		// ANT task output.
		output.println(text);
		// First, mirror indendation
		String str = "";
		for (int i = 0; i < start; ++i) {
			if (text.charAt(i) == '\t') {
				str += "\t";
			} else {
				str += " ";
			}
		}
		// Second, place highlights
		for (int i = start; i <= end; ++i) {
			str += "^";
		}
		output.println(str);
	}

	private static EnclosingLine readEnclosingLine(Path.Entry<?> entry, Attribute.Span location) {
		int spanStart = location.getStart().get().intValue();
		int spanEnd = location.getEnd().get().intValue();
		int line = 0;
		int lineStart = 0;
		int lineEnd = 0;
		StringBuilder text = new StringBuilder();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(entry.inputStream(), "UTF-8"));

			// first, read whole file
			int len = 0;
			char[] buf = new char[1024];
			while ((len = in.read(buf)) != -1) {
				text.append(buf, 0, len);
			}

			while (lineEnd < text.length() && lineEnd <= spanStart) {
				lineStart = lineEnd;
				lineEnd = parseLine(text, lineEnd);
				line = line + 1;
			}
		} catch (IOException e) {
			return null;
		}
		lineEnd = Math.min(lineEnd, text.length());

		return new EnclosingLine(spanStart, spanEnd, line, lineStart, lineEnd,
				text.substring(lineStart, lineEnd));
	}

	private static int parseLine(StringBuilder buf, int index) {
		while (index < buf.length() && buf.charAt(index) != '\n') {
			index++;
		}
		return index + 1;
	}

	private static class EnclosingLine {
		private int lineNumber;
		private int start;
		private int end;
		private int lineStart;
		private int lineEnd;
		private String lineText;

		public EnclosingLine(int start, int end, int lineNumber, int lineStart, int lineEnd, String lineText) {
			this.start = start;
			this.end = end;
			this.lineNumber = lineNumber;
			this.lineStart = lineStart;
			this.lineEnd = lineEnd;
			this.lineText = lineText;
		}

		public int columnStart() {
			return start - lineStart;
		}

		public int columnEnd() {
			return Math.min(end, lineEnd) - lineStart;
		}
	}
}
