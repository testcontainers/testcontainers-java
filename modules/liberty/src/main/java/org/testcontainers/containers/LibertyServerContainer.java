package org.testcontainers.containers;

import lombok.NonNull;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.applicationserver.ApplicationServerContainer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents an Open Liberty or WebSphere Liberty container
 */
public class LibertyServerContainer extends ApplicationServerContainer {

    public static final String NAME = "Liberty";

    // About the image
    public static final String IMAGE = "open-liberty";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(IMAGE);

    // Container defaults
    public static final int DEFAULT_HTTP_PORT = 9080;

    public static final int DEFAULT_HTTPS_PORT = 9443;

    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(30);

    private static final String SERVER_CONFIG_DIR = "/config/";

    private static final String APPLICATION_DROPIN_DIR = "/config/dropins/";

    private static final List<String> DEFAULT_FEATURES = Arrays.asList("webProfile-10.0");

    // Container fields
    private MountableFile serverConfiguration;

    private List<String> features = new ArrayList<>();

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

        // Copy server configuration
        if( Objects.nonNull(serverConfiguration) ) {
            withCopyFileToContainer(serverConfiguration, SERVER_CONFIG_DIR + "server.xml");
            return;
        }

        if ( ! features.isEmpty() ) {
            withCopyFileToContainer(generateServerConfiguration(features), SERVER_CONFIG_DIR + "server.xml");
            return;
        }

        withCopyFileToContainer(generateServerConfiguration(DEFAULT_FEATURES), SERVER_CONFIG_DIR + "server.xml");

    }

    @Override
    protected String getApplicationInstallDirectory() {
        return APPLICATION_DROPIN_DIR;
    }

    // Configuration

    /**
     * The server configuration file that will be copied to the Liberty container.
     *
     * Calling this method more than once will replace the existing serverConfig if set.
     *
     * @param serverConfig - server.xml
     * @return self
     */
    public LibertyServerContainer withServerConfiguration(@NonNull MountableFile serverConfig) {
        this.serverConfiguration = serverConfig;
        return this;
    }

    /**
     * A list of Liberty features to configure on the Liberty container.
     *
     * These features will be ignored if a serverConfig file is set.
     *
     * @param features - The list of features
     * @return self
     */
    public LibertyServerContainer withFeatures(String... features) {
        this.features.addAll(Arrays.asList(features));
        return this;
    }

    // Helpers

    private static final MountableFile generateServerConfiguration(List<String> features) {
        String configContents = "";
        configContents += "<server><featureManager>";
        for(String feature : features) {
            configContents += "<feature>" + feature + "</feature>";
        }
        configContents += "</featureManager></server>";
        configContents += System.lineSeparator();

        Path generatedConfigPath = Paths.get(getTempDirectory().toString(), "generatedServer.xml");

        try {
            Files.write(generatedConfigPath, configContents.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to generate server configuration at runtime", ioe);
        }

        return MountableFile.forHostPath(generatedConfigPath);
    }
}
