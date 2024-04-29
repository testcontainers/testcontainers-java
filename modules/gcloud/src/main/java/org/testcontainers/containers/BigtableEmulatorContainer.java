package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

/**
 * A Bigtable container that relies in google cloud sdk.
 * <p>
 * Supported images: {@code gcr.io/google.com/cloudsdktool/google-cloud-cli}, {@code gcr.io/google.com/cloudsdktool/cloud-sdk}
 * <p>
 * Default port is 9000.
 */
public class BigtableEmulatorContainer extends GenericContainer<BigtableEmulatorContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "gcr.io/google.com/cloudsdktool/google-cloud-cli"
    );

    private static final DockerImageName CLOUD_SDK_IMAGE_NAME = DockerImageName.parse(
        "gcr.io/google.com/cloudsdktool/cloud-sdk"
    );

    private static final String CMD = "gcloud beta emulators bigtable start --host-port 0.0.0.0:9000";

    private static final int PORT = 9000;

    public BigtableEmulatorContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public BigtableEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, CLOUD_SDK_IMAGE_NAME);

        withExposedPorts(PORT);
        setWaitStrategy(new LogMessageWaitStrategy().withRegEx("(?s).*running.*$"));
        withCommand("/bin/sh", "-c", CMD);
    }

    /**
     * @return a <code>host:port</code> pair corresponding to the address on which the emulator is
     * reachable from the test host machine. Directly usable as a parameter to the
     * com.google.cloud.ServiceOptions.Builder#setHost(java.lang.String) method.
     */
    public String getEmulatorEndpoint() {
        return getHost() + ":" + getEmulatorPort();
    }

    public int getEmulatorPort() {
        return getMappedPort(PORT);
    }
}
