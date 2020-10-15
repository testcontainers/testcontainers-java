package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

/**
 * A PubSub container that relies in google cloud sdk.
 *
 * Default port is 8085.
 *
 * @author Eddú Meléndez
 */
public class PubSubEmulatorContainer extends GenericContainer<PubSubEmulatorContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk");

    private static final String CMD = "gcloud beta emulators pubsub start --host-port 0.0.0.0:8085";
    private static final int PORT = 8085;

    public PubSubEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(8085);
        setWaitStrategy(new LogMessageWaitStrategy()
                .withRegEx("(?s).*started.*$"));
        withCommand("/bin/sh", "-c", CMD);
    }

    /**
     * @return a <code>host:port</code> pair corresponding to the address on which the emulator is
     * reachable from the test host machine. Directly usable as a parameter to the
     * io.grpc.ManagedChannelBuilder#forTarget(java.lang.String) method.
     */
    public String getEmulatorEndpoint() {
        return getContainerIpAddress() + ":" + getMappedPort(PORT);
    }
}
