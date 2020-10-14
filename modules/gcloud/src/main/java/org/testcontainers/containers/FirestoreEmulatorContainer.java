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

    public FirestoreEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(8080);
        setWaitStrategy(new LogMessageWaitStrategy()
                .withRegEx("(?s).*running.*$"));
        withCommand("/bin/sh", "-c", CMD);
    }

}
