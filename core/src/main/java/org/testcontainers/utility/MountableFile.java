package org.testcontainers.utility;

import com.google.common.base.Charsets;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.images.builder.Transferable;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static lombok.AccessLevel.PACKAGE;
import static org.testcontainers.utility.PathUtils.recursiveDeleteDir;

/**
 * An abstraction over files and classpath resources aimed at encapsulating all the complexity of generating
 * a path that the Docker daemon is about to create a volume mount for.
 */
@RequiredArgsConstructor(access = PACKAGE)
@Slf4j
public class MountableFile implements Transferable {

    private final String path;

    @Getter(lazy = true)
    private final String resolvedPath = resolvePath();

    /**
     * Obtains a {@link MountableFile} corresponding to a resource on the classpath (including resources in JAR files)
     *
     * @param resourceName the classpath path to the resource
     * @return a {@link MountableFile} that may be used to obtain a mountable path
     */
    public static MountableFile forClasspathResource(@NotNull final String resourceName) {
        return new MountableFile(getClasspathResource(resourceName, new HashSet<>()).toString());
    }

    /**
     * Obtains a {@link MountableFile} corresponding to a file on the docker host filesystem.
     *
     * @param path the path to the resource
     * @return a {@link MountableFile} that may be used to obtain a mountable path
     */
    public static MountableFile forHostPath(@NotNull final String path) {
        return new MountableFile(new File(path).toURI().toString());
    }

    /**
     * Obtains a {@link MountableFile} corresponding to a file on the docker host filesystem.
     *
     * @param path the path to the resource
     * @return a {@link MountableFile} that may be used to obtain a mountable path
     */
    public static MountableFile forHostPath(final Path path) {
        return new MountableFile(path.toAbsolutePath().toString());
    }

    @NotNull
    private static URL getClasspathResource(@NotNull final String resourcePath, @NotNull final Set<ClassLoader> classLoaders) {

        final Set<ClassLoader> classLoadersToSearch = new HashSet<>(classLoaders);
        // try context and system classloaders as well
        classLoadersToSearch.add(Thread.currentThread().getContextClassLoader());
        classLoadersToSearch.add(ClassLoader.getSystemClassLoader());
        classLoadersToSearch.add(MountableFile.class.getClassLoader());

        for (final ClassLoader classLoader : classLoadersToSearch) {
            URL resource = classLoader.getResource(resourcePath);
            if (resource != null) {
                return resource;
            }

            // Be lenient if an absolute path was given
            if (resourcePath.startsWith("/")) {
                resource = classLoader.getResource(resourcePath.replaceFirst("/", ""));
                if (resource != null) {
                    return resource;
                }
            }
        }

        throw new IllegalArgumentException("Resource with path " + resourcePath + " could not be found on any of these classloaders: " + classLoadersToSearch);
    }

    private static String unencodeResourceURIToFilePath(@NotNull final String resource) {
        try {
            // Convert any url-encoded characters (e.g. spaces) back into unencoded form
            return URLDecoder.decode(resource, Charsets.UTF_8.name())
                    .replaceFirst("jar:", "")
                    .replaceFirst("file:", "")
                    .replaceAll("!.*", "");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Obtain a path that the Docker daemon should be able to use to volume mount a file/resource
     * into a container. If this is a classpath resource residing in a JAR, it will be extracted to
     * a temporary location so that the Docker daemon is able to access it.
     *
     * @return a volume-mountable path.
     */
    private String resolvePath() {
        String result;
        if (path.contains(".jar!")) {
            result = extractClassPathResourceToTempLocation(this.path);
        } else {
            result = unencodeResourceURIToFilePath(path);
        }

        if (SystemUtils.IS_OS_WINDOWS) {
            result = PathUtils.createMinGWPath(result);
        }

        return result;
    }

    /**
     * Extract a file or directory tree from a JAR file to a temporary location.
     * This allows Docker to mount classpath resources as files.
     *
     * @param hostPath the path on the host, expected to be of the format 'file:/path/to/some.jar!/classpath/path/to/resource'
     * @return the path of the temporary file/directory
     */
    private String extractClassPathResourceToTempLocation(final String hostPath) {
        File tmpLocation = new File(".testcontainers-tmp-" + Base58.randomString(5));
        //noinspection ResultOfMethodCallIgnored
        tmpLocation.delete();

        String urldecodedJarPath = unencodeResourceURIToFilePath(hostPath);
        String internalPath = hostPath.replaceAll("[^!]*!/", "");

        try (JarFile jarFile = new JarFile(urldecodedJarPath)) {
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
    private void copyFromJarToLocation(final JarFile jarFile,
                                       final JarEntry entry,
                                       final String fromRoot,
                                       final File toRoot) throws IOException {

        String destinationName = entry.getName().replaceFirst(fromRoot, "");
        File newFile = new File(toRoot, destinationName);

        log.debug("Copying resource {} from JAR file {}",
                fromRoot,
                jarFile.getName());

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

    private void deleteOnExit(final Path path) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> recursiveDeleteDir(path)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transferTo(final TarArchiveOutputStream outputStream, String destinationPathInTar) {
        recursiveTar(destinationPathInTar, this.getResolvedPath(), this.getResolvedPath(), outputStream);
    }

    /*
     * Recursively copies a file/directory into a TarArchiveOutputStream
     */
    private void recursiveTar(String destination, String sourceRootDir, String sourceCurrentItem, TarArchiveOutputStream tarArchive) {
        try {
            final File sourceFile = new File(sourceCurrentItem).getCanonicalFile();     // e.g. /foo/bar/baz
            final File sourceRootFile = new File(sourceRootDir).getCanonicalFile();     // e.g. /foo
            final String relativePathToSourceFile = sourceRootFile.toPath().relativize(sourceFile.toPath()).toFile().toString();    // e.g. /bar/baz

            final TarArchiveEntry tarEntry = new TarArchiveEntry(sourceFile, destination + "/" + relativePathToSourceFile); // entry filename e.g. /xyz/bar/baz

            // TarArchiveEntry automatically sets the mode for file/directory, but we can update to ensure that the mode is set exactly (inc executable bits)
            tarEntry.setMode(getUnixFileMode(sourceCurrentItem));
            tarArchive.putArchiveEntry(tarEntry);

            if (sourceFile.isFile()) {
                Files.copy(sourceFile.toPath(), tarArchive);
            }
            // a directory entry merely needs to exist in the TAR file - there is no data stored yet
            tarArchive.closeArchiveEntry();

            final File[] children = sourceFile.listFiles();
            if (children != null) {
                // recurse into child files/directories
                for (final File child : children) {
                    recursiveTar(destination, sourceRootDir + File.separator, child.getCanonicalPath(), tarArchive);
                }
            }
        } catch (IOException e) {
            log.error("Error when copying TAR file entry: {}", sourceCurrentItem, e);
            throw new UncheckedIOException(e); // fail fast
        }
    }

    @Override
    public long getSize() {

        final File file = new File(this.getResolvedPath());
        if (file.isFile()) {
            return file.length();
        } else {
            return 0;
        }
    }

    @Override
    public String getDescription() {
        return this.getResolvedPath();
    }

    @Override
    public int getFileMode() {
        return getUnixFileMode(this.getResolvedPath());
    }

    private int getUnixFileMode(final String pathAsString) {
        final Path path = Paths.get(pathAsString);
        try {
            return (int) Files.getAttribute(path, "unix:mode");
        } catch (IOException e) {
            // fallback for non-posix environments
            int mode = DEFAULT_FILE_MODE;
            if (Files.isDirectory(path)) {
                mode = DEFAULT_DIR_MODE;
            } else if (Files.isExecutable(path)) {
                mode |= 0111; // equiv to +x for user/group/others
            }

            return mode;
        }
    }
}