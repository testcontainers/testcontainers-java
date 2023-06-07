package org.testcontainers.containers;

import lombok.NonNull;
import org.apache.commons.lang3.SystemUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a JavaEE, JakartaEE, or Microprofile application platform running inside
 * a Docker container
 */
public abstract class ApplicationContainer extends GenericContainer<ApplicationContainer> {

    // List of applications to install
    List<MountableFile> archives = new ArrayList<>();

    // Where to save runtime application archives
    private static final String TESTCONTAINERS_TMP_DIR_PREFIX = ".testcontainers-archive-";

    private static final String OS_MAC_TMP_DIR = "/tmp";

    private static Path tempDirectory;

    // How to query application
    private Integer httpPort;

    private String appContextRoot;

    // How to query application for readiness
    private Integer readinessPort;

    private String readinessPath;

    private Duration readinessTimeout;

    // Expected path for Microprofile platforms to query for readiness
    static final String MP_HEALTH_READINESS_PATH = "/health/ready";

    //Constructors

    public ApplicationContainer(@NonNull final Future<String> image) {
        super(image);
    }

    public ApplicationContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    //Overrides

    @Override
    protected void configure() {
        super.configure();

        // Setup default wait strategy
        waitingFor(
            Wait.forHttp(readinessPath != null ? readinessPath : appContextRoot)
                .forPort(readinessPort != null ? readinessPort : httpPort)
                .withStartupTimeout(readinessTimeout != null ? readinessTimeout : getDefaultWaitTimeout())
        );

        // Copy applications
        for(MountableFile archive : archives) {
            withCopyFileToContainer(archive, getApplicationInstallDirectory() + extractApplicationName(archive));
        }
    }

    @Override
    protected void containerIsCreated(String containerId) {
        if( Objects.isNull(tempDirectory) ) {
            return;
        }

        try {
            //Delete files in temp directory
            for(String file : tempDirectory.toFile().list()) {
                Files.deleteIfExists(Paths.get(tempDirectory.toString(), file));
            }
            //Delete temp directory
            Files.deleteIfExists(tempDirectory);
        } catch (IOException e) {
            logger().info("Unable to delete temporary directory " + tempDirectory.toString(), e);
        }

    }

    @Override
    public void setExposedPorts(List<Integer> exposedPorts) {
        if( Objects.isNull(this.httpPort) ) {
            super.setExposedPorts(exposedPorts);
            return;
        }

        super.setExposedPorts(appendPort(exposedPorts, this.httpPort));
    }

    //Configuration

    /**
     * One or more archives to be deployed to the application platform
     *
     * @param archives - An archive created using shrinkwrap and test runtime
     * @return self
     */
    public ApplicationContainer withApplicationArchvies(@NonNull Archive<?>... archives) {
        Stream.of(archives).forEach(archive -> {
            String name = archive.getName();
            Path target = Paths.get(getTempDirectory().toString(), name);
            archive.as(ZipExporter.class).exportTo(target.toFile(), true);
            this.archives.add(MountableFile.forHostPath(target));
        });

        return this;
    }

    /**
     * One or more archives to be deployed to the application platform
     *
     * @param archives - A MountableFile which represents an archive that was created prior to test runtime.
     * @return self
     */
    public ApplicationContainer withApplicationArchives(@NonNull MountableFile... archives) {
        this.archives.addAll(Arrays.asList(archives));
        return this;
    }

    /**
     * This will set the port used to construct the base application URL, as well as
     * the port used to determine container readiness in the case of Microprofile platforms.
     *
     * @param httpPort - The HTTP port used by the Application platform
     * @return self
     */
    public ApplicationContainer withHttpPort(int httpPort) {
        this.httpPort = httpPort;
        super.setExposedPorts(appendPort(getExposedPorts(), this.httpPort));

        return this;
    }

    /**
     * This will set the path used to construct the base application URL
     *
     * @param appContextRoot - The application path
     * @return self
     */
    public ApplicationContainer withAppContextRoot(@NonNull String appContextRoot) {
        this.appContextRoot = normalizePath(appContextRoot);
        return this;
    }

    /**
     * Sets the path to be used to determine container readiness.
     * This will be used to construct a default WaitStrategy using Wait.forHttp()
     *
     * @param readinessPath - The path to be polled for readiness.
     * @return self
     */
    public ApplicationContainer withReadinessPath(String readinessPath) {
        return withReadinessPath(readinessPath, getDefaultWaitTimeout());
    }

    /**
     * Sets the path to be used to determine container readiness.
     * The readiness check will timeout a user defined timeout.
     * This will be used to construct a default WaitStrategy using Wait.forHttp()
     *
     * @param readinessPath - The path to be polled for readiness.
     * @param timeout - The timeout after which the readiness check will fail.
     * @return self
     */
    public ApplicationContainer withReadinessPath(String readinessPath, Duration timeout) {
        return withReadinessPath(readinessPath, httpPort, timeout);
    }

    /**
     * Sets the path to be used to determine container readiness.
     * The readiness check will timeout a user defined timeout.
     * The readiness check will happen on an alternative port to the httpPort
     *
     * @param readinessPath - The path to be polled for readiness.
     * @param readinessPort - The port that should be used for the readiness check.
     * @param timeout - The timeout after which the readiness check will fail.
     * @return self
     */
    public ApplicationContainer withReadinessPath(@NonNull String readinessPath, @NonNull Integer readinessPort, @NonNull Duration timeout) {
        this.readinessPath = normalizePath(readinessPath);
        this.readinessPort = readinessPort;
        return this;
    }

    //Getters

    /**
     * The URL used to determine container readiness.
     * If a readiness port and path were configured, those will be used to construct this URL.
     * Otherwise, the readiness path will default to the httpPort and application context root.
     *
     * @return - The readiness URL
     */
    public String getReadinessURL() {
        if ( Objects.isNull(this.readinessPath) || Objects.isNull(this.readinessPort) ) {
            return getApplicationURL();
        }

        return "http://" + getHost() + ':' + getMappedPort(this.readinessPort) + this.readinessPath;
    }

    /**
     * The URL where the application is running.
     * The application URL is a concatenation of the baseURL {@link ApplicationContainer#getBaseURL()} with the appContextRoot.
     * This is the URL that the test client should use to connect to an application running on the application platform.
     *
     * @return - The application URL
     */
    public String getApplicationURL() {
        Objects.requireNonNull(this.appContextRoot);
        return getBaseURL() + appContextRoot;
    }

    /**
     * The base URL for the application platform.
     * This is the URL that the test client should use to connect to the application platform.
     *
     * @return - The base URL
     */
    public String getBaseURL() {
        Objects.requireNonNull(this.httpPort);
        return "http://" + getHost() + ':' + getMappedPort(this.httpPort);
    }

    // Abstract

    /**
     * Each implementation will need to determine an appropriate amount of time
     * to wait for their application platform to start.
     *
     * @return - Duration to wait for startup
     */
    abstract protected Duration getDefaultWaitTimeout();

    /**
     * Each implementation will need to provide an install directory
     * where their platform expects applications to be copied into.
     *
     * @return - the application install directory
     */
    abstract protected String getApplicationInstallDirectory();

    // Helpers

    /**
     * Create a temporary directory where runtime archives can be exported and copied to the application container.
     *
     * @return - The temporary directory path
     */
    private static Path getTempDirectory() {
        if( Objects.nonNull(tempDirectory) ) {
            return tempDirectory;
        }

        try {
            if (SystemUtils.IS_OS_MAC) {
                tempDirectory = Files.createTempDirectory(Paths.get(OS_MAC_TMP_DIR), TESTCONTAINERS_TMP_DIR_PREFIX);
            } else {
                tempDirectory = Files.createTempDirectory(TESTCONTAINERS_TMP_DIR_PREFIX);
            }
        } catch (IOException e) {
            tempDirectory = new File(TESTCONTAINERS_TMP_DIR_PREFIX + Base58.randomString(5)).toPath();
        }

        return tempDirectory;
    }

    private static String extractApplicationName(MountableFile file) throws IllegalArgumentException {
        String path = file.getFilesystemPath();
        if( path.matches(".*\\.jar|.*\\.war|.*\\.ear") ) {
            return path.substring(path.lastIndexOf("/"));
        }
        throw new IllegalArgumentException("File did not contain an application archive");
    }

    /**
     * Appends a port to a list of ports at position 0 and ensures port list does
     * not contain duplicates to ensure this order is maintained.
     *
     * @param portList - List of existing ports
     * @param appendingPort - The port to append
     * @return - resulting list of ports
     */
    protected static List<Integer> appendPort(List<Integer> portList, int appendingPort) {
        portList = new ArrayList<>(portList); //Ensure not immutable
        portList.add(0, appendingPort);
        return portList.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Normalizes the path provided by developer input.
     * - Ensures paths start with '/'
     * - Ensures paths are separated with exactly one '/'
     * - Ensures paths do not end with a '/'
     *
     * @param paths the list of paths to be normalized
     * @return The normalized path
     */
    protected static String normalizePath(String... paths) {
        return Stream.of(paths)
            .flatMap(path -> Stream.of(path.split("/")))
            .filter(part -> !part.isEmpty())
            .collect(Collectors.joining("/", "/", ""));
    }

}
