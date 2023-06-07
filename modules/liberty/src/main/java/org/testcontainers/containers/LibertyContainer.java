package org.testcontainers.containers;

import lombok.NonNull;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Future;

/**
 * Represents an Open Liberty or WebSphere Liberty container
 */
public class LibertyContainer extends ApplicationContainer {

    public static final String NAME = "Liberty";

    // About the image
    static final String IMAGE = "open-liberty";

    static final String DEFAULT_TAG = "23.0.0.3-full-java17-openj9";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(IMAGE);

    // Container defaults
    public static final int DEFAULT_HTTP_PORT = 9080;

    public static final int DEFAULT_HTTPS_PORT = 9443;

    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(30);

    private static final String SERVER_CONFIG_DIR = "/config/";

    private static final String APPLICATION_DROPIN_DIR = "/config/dropins/";

    // Container fields
    @NonNull
    private MountableFile serverConfiguration = MountableFile.forClasspathResource("default/config/defaultServer.xml");

    // Constructors

    public LibertyContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public LibertyContainer(@NonNull Future<String> image) {
        super(image);
        preconfigure();
    }

    public LibertyContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        preconfigure();
    }

    // Overrides

    @Override
    public void configure() {
        super.configure();

        // Copy server configuration
        Objects.requireNonNull(serverConfiguration);
        withCopyFileToContainer(serverConfiguration, SERVER_CONFIG_DIR + "server.xml");

    }

    @Override
    protected Duration getDefaultWaitTimeout() {
        return DEFAULT_WAIT_TIMEOUT;
    }

    @Override
    protected String getApplicationInstallDirectory() {
        return APPLICATION_DROPIN_DIR;
    }

    // Configuration

    /**
     * Setup default configurations that can be overridden by users
     */
    private void preconfigure() {
        withHttpPort(DEFAULT_HTTP_PORT);
    }

    /**
     * The server configuration file that will be copied to the Liberty container
     *
     * @param serverConfig - server.xml
     * @return self
     */
    public LibertyContainer withServerConfiguration(@NonNull MountableFile serverConfig) {
        this.serverConfiguration = serverConfig;
        return this;
    }
}
