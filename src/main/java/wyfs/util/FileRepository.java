package wyfs.util;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import wycc.lang.Build;
import wycc.lang.Path;
import wycc.util.AbstractRepository;
import wycc.util.ArrayUtils;
import wyfs.lang.Content;
import wyfs.lang.FileSystem;

public class FileRepository extends AbstractRepository {

    public final static FileFilter NULL_FILTER = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return true;
        }
    };

    private final FileFilter filter;
    private final Content.Registry registry;
    private final File dir;
    private final HashSet<Path> dirty;

    public FileRepository(Content.Registry registry, File dir) throws IOException {
        this(registry, dir, NULL_FILTER);
    }

    public FileRepository(Content.Registry registry, File dir, FileFilter filter) throws IOException {
        super(initialise(registry, dir, filter));
        this.filter = filter;
        this.registry = registry;
        this.dir = dir;
        this.dirty = new HashSet<>();
    }

    /**
     * Flush all files to disk
     */
    public void flush() throws IOException {
        State st = get();
        for (Build.Artifact e : st) {
            // FIXME: need to modify Content.Registry to determine content type from artifact.
            flush(dir, e, null);
        }
    }

    /**
     * Get the root directory where this repository starts from.
     *
     * @return
     */
    public File getDirectory() {
        return dir;
    }

    @Override
	public String toString() {
    	String r = "{";
    	boolean firstTime = true;
    	for(Build.Artifact f : get()) {
    		if(!firstTime) {
    			r += ",";
    		}
    		r += f.getID();
    		firstTime = false;
    	}
    	return r + "}";
    }

    /**
     * Construct the initial state from the contents of the build directory.
     *
     * @param registry
     * @param dir
     * @param filter
     * @return
     * @throws IOException
     */
    private static State initialise(Content.Registry registry, File dir, FileFilter filter) throws IOException {
        java.nio.file.Path root = dir.toPath();
        // First extract all files rooted in this directory
        List<File> files = findAll(64, dir, filter, new ArrayList<>());
        // Second convert them all into entries as appropriate
        Build.Artifact[] entries = new Build.Artifact[files.size()];
        //
        for (int i = 0; i != entries.length; ++i) {
            File ith = files.get(i);
            String filename = root.relativize(ith.toPath()).toString().replace(File.separatorChar, '/');
            entries[i] = read(filename, files.get(i), registry);
        }
        // Remove any files we didn't recognise.
        entries = ArrayUtils.removeAll(entries, null);
        // Done
        return new State(entries);
    }

    /**
     * Read a given file from the filesystem and convert it into an entry using the given content registry. If the
     * registry doesn't recognised the file, then this just returns <code>null</code>.
     *
     * @param filename
     * @param file
     * @param registry
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    private static Build.Artifact read(String filename, File file, Content.Registry registry) throws IOException {
        // Determine file suffix
        String suffix = "";
        int pos = filename.lastIndexOf('.');
        if (pos > 0) {
            suffix = filename.substring(pos + 1);
        }
        // Compute ID
        Path id = Path.fromString(filename.substring(0, filename.length() - (suffix.length() + 1)));
        // Extract appropriate content type (if applicable)
        Content.Type<?> type = registry.contentType(suffix);
        // Read entry (if applicable)
        if (type == null) {
            return null;
        } else {
            // FIXME: this cast should not be necessary!
            return (Build.Artifact) type.read(id, new FileInputStream(file));
        }
    }

    /**
     * Extract all files starting from a given directory.
     *
     * @param dir
     * @return
     */
    private static List<File> findAll(int n, File dir, FileFilter filter, List<File> files) {
        if (n > 0 && dir.exists() && dir.isDirectory()) {
            File[] contents = dir.listFiles(filter);
            for (int i = 0; i != contents.length; ++i) {
                File ith = contents[i];
                //
                if (ith.isDirectory()) {
                    findAll(n - 1, ith, filter, files);
                } else {
                    files.add(ith);
                }
            }
        }
        return files;
    }

    private static <T extends Build.Artifact> boolean flush(File dir, T e, Content.Type<T> ct) throws IOException {
        // Convert ID into filesystem path
        String filename = e.getID().toString().replace("/", File.separator) + "." + ct.getSuffix();
        // Done.
        File f = new File(dir, filename);
        //
        if (!f.exists() && !f.createNewFile()) {
            // Error creating file occurred
            return false;
        } else {
            FileOutputStream fout = new FileOutputStream(f);
            ct.write(fout, e);
            fout.close();
            return true;
        }
    }
}
