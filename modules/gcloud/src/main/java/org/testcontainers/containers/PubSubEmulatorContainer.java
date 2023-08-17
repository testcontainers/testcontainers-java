package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

/**
 * A PubSub container that relies in google cloud sdk.
 * <p>
 * Supported images: {@code gcr.io/google.com/cloudsdktool/google-cloud-cli}, {@code gcr.io/google.com/cloudsdktool/cloud-sdk}
 * <p>
 * Default port is 8085.
 */
public class PubSubEmulatorContainer extends GenericContainer<PubSubEmulatorContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "gcr.io/google.com/cloudsdktool/google-cloud-cli"
    );

    private static final DockerImageName CLOUD_SDK_IMAGE_NAME = DockerImageName.parse(
        "gcr.io/google.com/cloudsdktool/cloud-sdk"
    );

    private static final String CMD = "gcloud beta emulators pubsub start --host-port 0.0.0.0:8085";

    private static final int PORT = 8085;

    public PubSubEmulatorContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public PubSubEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, CLOUD_SDK_IMAGE_NAME);

        withExposedPorts(8085);
        setWaitStrategy(new LogMessageWaitStrategy().withRegEx("(?s).*started.*$"));
        withCommand("/bin/sh", "-c", CMD);
    }

    /**
     * @return a <code>host:port</code> pair corresponding to the address on which the emulator is
     * reachable from the test host machine. Directly usable as a parameter to the
     * io.grpc.ManagedChannelBuilder#forTarget(java.lang.String) method.
     */
    public String getEmulatorEndpoint() {
        return getHost() + ":" + getMappedPort(PORT);
    }
}
