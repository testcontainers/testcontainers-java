package org.testcontainers.utility;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Filesystem operation utility methods.
 */
@UtilityClass
public class PathUtils {

    /**
     * Recursively delete a directory and all its subdirectories and files.
     *
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
     *
     * @param path a usual windows path
     * @return a MinGW compatible path
     */
    public static String createMinGWPath(String path) {
        String mingwPath = path.replace('\\', '/');
        int driveLetterIndex = 1;
        if (mingwPath.matches("^[a-zA-Z]:\\/.*")) {
            driveLetterIndex = 0;
        }

        // drive-letter must be lower case
        mingwPath = "//" + Character.toLowerCase(mingwPath.charAt(driveLetterIndex)) +
                mingwPath.substring(driveLetterIndex + 1);
        mingwPath = mingwPath.replace(":", "");
        return mingwPath;
    }
}
