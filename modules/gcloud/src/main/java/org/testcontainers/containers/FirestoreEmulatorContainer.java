package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * A Firestore container that relies in google cloud sdk.
 * <p>
 * Supported images: {@code gcr.io/google.com/cloudsdktool/google-cloud-cli}, {@code gcr.io/google.com/cloudsdktool/cloud-sdk}
 * <p>
 * Default port is 8080.
 */
public class FirestoreEmulatorContainer extends GenericContainer<FirestoreEmulatorContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "gcr.io/google.com/cloudsdktool/google-cloud-cli"
    );

    private static final DockerImageName CLOUD_SDK_IMAGE_NAME = DockerImageName.parse(
        "gcr.io/google.com/cloudsdktool/cloud-sdk"
    );

    private static final String CMD = "gcloud beta emulators firestore start --host-port 0.0.0.0:8080";

    private static final int PORT = 8080;

    public FirestoreEmulatorContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public FirestoreEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, CLOUD_SDK_IMAGE_NAME);

        withExposedPorts(PORT);
        setWaitStrategy(Wait.forLogMessage(".*running.*$", 1));
        withCommand("/bin/sh", "-c", CMD);
    }

    /**
     * @return a <code>host:port</code> pair corresponding to the address on which the emulator is
     * reachable from the test host machine. Directly usable as a parameter to the
     * com.google.cloud.ServiceOptions.Builder#setHost(java.lang.String) method.
     */
    public String getEmulatorEndpoint() {
        return getHost() + ":" + getMappedPort(8080);
    }
}
