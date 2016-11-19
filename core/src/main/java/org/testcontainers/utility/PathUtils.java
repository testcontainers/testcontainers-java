package org.testcontainers.utility;

import lombok.NonNull;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Filesystem operation utility methods.
 */
public class PathUtils {

    private static final Logger log = getLogger(PathUtils.class);

    /**
     * Recursively delete a directory and all its subdirectories and files.
     * @param directory path to the directory to delete.
     */
    public static void recursiveDeleteDir(final @NonNull Path directory) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
        }
    }

    /**
     * Make a directory, plus any required parent directories.
     *
     * @param directory the directory path to make
     */
    public static void mkdirp(Path directory) {
        boolean result = directory.toFile().mkdirs();
        if (!result) {
            throw new IllegalStateException("Failed to create directory at: " + directory);
        }
    }
    
    /**
     * Create a MinGW compatible path based on usual Windows path
     * @param path a usual windows path 
     * @return a MinGW compatible path
     */
    public static String createMinGWPath(String path) {
    	String mingwPath = path.replace('\\', '/');
    	int driveLetterIndex = 1;
        if(mingwPath.matches("^[a-zA-Z]:\\/.*")) {
        	driveLetterIndex = 0;
        }
        
        // drive-letter must be lower case
        mingwPath = "//" + Character.toLowerCase(mingwPath.charAt(driveLetterIndex)) +
        		mingwPath.substring(driveLetterIndex+1);
        mingwPath = mingwPath.replace(":","");
        return mingwPath;
    }

    /**
     * Extract a file or directory tree from a JAR file to a temporary location.
     * This allows Docker to mount classpath resources as files.
     * @param hostPath the path on the host, expected to be of the format 'file:/path/to/some.jar!/classpath/path/to/resource'
     * @return the path of the temporary file/directory
     */
    public static String extractClassPathResourceToTempLocation(final String hostPath) {
        File tmpLocation = new File(".testcontainers-tmp-" + Base58.randomString(5));
        //noinspection ResultOfMethodCallIgnored
        tmpLocation.delete();

        String jarPath = hostPath.replaceFirst("file:", "").replaceAll("!.*", "");
        String internalPath = hostPath.replaceAll("[^!]*!/", "");

        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                final String name = entry.getName();
                if (name.startsWith(internalPath)) {
                    log.debug("Copying classpath resource(s) from {} to {} to permit Docker to bind",
                            hostPath,
                            tmpLocation);
                    copyFromJarToLocation(jarFile, entry, internalPath, tmpLocation);
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("Failed to process JAR file when extracting classpath resource: " + hostPath, e);
        }

        // Mark temporary files/dirs for deletion at JVM shutdown
        deleteOnExit(tmpLocation.toPath());

        return tmpLocation.getAbsolutePath();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void copyFromJarToLocation(final JarFile jarFile,
                                              final JarEntry entry,
                                              final String fromRoot,
                                              final File toRoot) throws IOException {

        String destinationName = entry.getName().replaceFirst(fromRoot, "");
        File newFile = new File(toRoot, destinationName);

        if (!entry.isDirectory()) {
            // Create parent directories
            newFile.mkdirs();
            newFile.delete();
            newFile.deleteOnExit();

            try (InputStream is = jarFile.getInputStream(entry)) {
                Files.copy(is, newFile.toPath());
            } catch (IOException e) {
                log.error("Failed to extract classpath resource " + entry.getName() + " from JAR file " + jarFile.getName(), e);
                throw e;
            }
        }
    }

    public static void deleteOnExit(final Path path) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> recursiveDeleteDir(path)));
    }
}
