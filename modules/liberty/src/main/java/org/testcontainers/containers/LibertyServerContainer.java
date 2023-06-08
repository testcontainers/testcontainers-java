package org.testcontainers.containers;

import lombok.NonNull;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.applicationserver.ApplicationServerContainer;

import java.time.Duration;
import java.util.Objects;

/**
 * Represents an Open Liberty or WebSphere Liberty container
 */
public class LibertyServerContainer extends ApplicationServerContainer {

    public static final String NAME = "Liberty";

    // About the image
    public static final String IMAGE = "open-liberty";

    public static final String DEFAULT_TAG = "23.0.0.3-full-java17-openj9";

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
    public LibertyServerContainer(String imageName) {
        this(DockerImageName.parse(imageName));
    }

    public LibertyServerContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        preconfigure();
    }

    /**
     * Configure defaults that can be overridden by developer prior to start()
     */
    private void preconfigure() {
        withHttpPort(DEFAULT_HTTP_PORT);
        withHttpWaitTimeout(DEFAULT_WAIT_TIMEOUT);
    }

    // Overrides

    @Override
    public void configure() {
        super.configure();

        // Copy default server configuration
        Objects.requireNonNull(serverConfiguration);
        withCopyFileToContainer(serverConfiguration, SERVER_CONFIG_DIR + "server.xml");
    }

    @Override
    protected String getApplicationInstallDirectory() {
        return APPLICATION_DROPIN_DIR;
    }

    // Configuration

    /**
     * The server configuration file that will be copied to the Liberty container
     *
     * @param serverConfig - server.xml
     * @return self
     */
    public LibertyServerContainer withServerConfiguration(@NonNull MountableFile serverConfig) {
        System.out.println("KJA1017 serverConfig called: " + serverConfig.getFilesystemPath());
        this.serverConfiguration = serverConfig;
        return this;
    }
}
