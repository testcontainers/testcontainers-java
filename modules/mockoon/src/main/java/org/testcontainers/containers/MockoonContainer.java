package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Files;
import java.nio.file.Path;

public class MockoonContainer extends GenericContainer<MockoonContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("mockoon/cli");

    private static final String DEFAULT_TAG = "3.0.0";

    @Deprecated
    public static final String VERSION = DEFAULT_TAG;

    public static final int PORT = 3000;

    private static final String DATA_MOUNT = "/data";

    /**
     * @deprecated use {@link MockoonContainer(DockerImageName, Path)} instead
     */
    public MockoonContainer(Path path) {
        this(DEFAULT_IMAGE_NAME, path);
    }

    public MockoonContainer(DockerImageName dockerImageName, Path path) {
        super(dockerImageName);
        commonInitialization(dockerImageName);
        if (path != null && Files.exists(path)) {
            withFileSystemBind(path.toString(), DATA_MOUNT);
        } else {
            throw new IllegalArgumentException("Path does not exist: " + path);
        }
    }

    /**
     * @deprecated use {@link MockoonContainer(DockerImageName, String)} instead
     */
    public MockoonContainer(String classpathResource) {
        this(DEFAULT_IMAGE_NAME, classpathResource);
    }

    public MockoonContainer(DockerImageName dockerImageName, String classpathResource) {
        super(dockerImageName);
        commonInitialization(dockerImageName);
        if (classpathResource != null) {
            // withClasspathResourceMapping checks if the resource cannot be found; no further checking here
            withClasspathResourceMapping(classpathResource, DATA_MOUNT, BindMode.READ_ONLY);
        } else {
            throw new IllegalArgumentException("Classpath resource must be specified");
        }
    }

    private void commonInitialization(DockerImageName dockerImageName) {
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        waitingFor(Wait.forLogMessage(".*Server started on port " + PORT + ".*", 1));

        withCommand("--data", DATA_MOUNT, "--port", Integer.toString(PORT));

        addExposedPorts(PORT);
    }

    public String getEndpoint() {
        return String.format("http://%s:%d", getHost(), getMappedPort(PORT));
    }

    /**
     * Mockoon in Docker doesn't (yet?) support HTTPS.
     */
//    public String getSecureEndpoint() {
//        return String.format("https://%s:%d", getHost(), getMappedPort(PORT));
//    }

    public Integer getServerPort() {
        return getMappedPort(PORT);
    }
}
