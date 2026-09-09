package org.testcontainers.utility;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

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
            Files.walkFileTree(
                directory,
                new SimpleFileVisitor<Path>() {
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
                }
            );
        } catch (IOException ignored) {}
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
        mingwPath =
            "//" +
            Character.toLowerCase(mingwPath.charAt(driveLetterIndex)) +
            mingwPath.substring(driveLetterIndex + 1);
        mingwPath = mingwPath.replace(":", "");
        return mingwPath;
    }

    /**
     * Find the closest common ancestor directory of a set of files, e.g. so that a single directory tree can be
     * used to resolve all of the files, even when they were supplied from different directories.
     *
     * @param files the files to find a common ancestor directory for
     * @return the closest common ancestor directory of all the given files
     */
    public static File findCommonParent(@NonNull List<File> files) {
        List<Path> parentDirs = files
            .stream()
            .map(file -> file.getAbsoluteFile().toPath().normalize().getParent())
            .collect(Collectors.toList());

        Path commonParent = parentDirs.get(0);
        for (Path parentDir : parentDirs) {
            commonParent = commonAncestor(commonParent, parentDir);
        }
        return commonParent.toFile();
    }

    private static Path commonAncestor(Path a, Path b) {
        if (!a.getRoot().equals(b.getRoot())) {
            throw new IllegalArgumentException(
                "Cannot determine a common parent directory for paths on different filesystem roots: " + a + " and " + b
            );
        }

        Path commonAncestor = a.getRoot();
        int sharedNameCount = Math.min(a.getNameCount(), b.getNameCount());
        for (int i = 0; i < sharedNameCount; i++) {
            Path aElement = a.getName(i);
            if (!aElement.equals(b.getName(i))) {
                break;
            }
            commonAncestor = commonAncestor.resolve(aElement);
        }
        return commonAncestor;
    }
}
