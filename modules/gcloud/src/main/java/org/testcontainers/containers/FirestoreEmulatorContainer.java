package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

/**
 * A Firestore container that relies in google cloud sdk.
 *
 * Default port is 8080.
 *
 * @author Eddú Meléndez
 */
public class FirestoreEmulatorContainer extends GCloudGenericContainer<FirestoreEmulatorContainer> {

    private static final String CMD = "gcloud beta emulators firestore start --host-port 0.0.0.0:8080";

    public FirestoreEmulatorContainer(String image) {
        super(image);
        withExposedPorts(8080);
        setWaitStrategy(new LogMessageWaitStrategy()
                .withRegEx("(?s).*running.*$"));
        withCommand("/bin/sh", "-c", CMD);
    }

    public FirestoreEmulatorContainer() {
        this(DEFAULT_GCLOUD_IMAGE);
    }

}
