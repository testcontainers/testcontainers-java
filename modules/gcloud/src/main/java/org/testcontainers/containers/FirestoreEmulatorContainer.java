package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

/**
 * A Firestore container that relies in google cloud sdk.
 *
 * Default port is 8080.
 *
 * @author Eddú Meléndez
 */
public class FirestoreEmulatorContainer extends GenericContainer<FirestoreEmulatorContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk");

    private static final String CMD = "gcloud beta emulators firestore start --host-port 0.0.0.0:8080";
    private static final int PORT = 8080;

    public FirestoreEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(PORT);
        setWaitStrategy(new LogMessageWaitStrategy()
                .withRegEx("(?s).*running.*$"));
        withCommand("/bin/sh", "-c", CMD);
    }

    /**
     * @return a <code>host:port</code> pair corresponding to the address on which the emulator is
     * reachable from the test host machine. Directly usable as a parameter to the
     * com.google.cloud.ServiceOptions.Builder#setHost(java.lang.String) method.
     */
    public String getEmulatorEndpoint() {
        return getContainerIpAddress() + ":" + getMappedPort(8080);
    }
}
