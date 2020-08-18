package org.testcontainers.containers;

import java.time.Duration;
import java.util.Arrays;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

/**
 * A Firestore container that relies in google cloud sdk. The container provides
 * additional instructions to install the components needed in the alpine images.
 *
 * Default port is 8080.
 *
 * @author Eddú Meléndez
 */
public class FirestoreEmulatorContainer extends GCloudGenericContainer<FirestoreEmulatorContainer> {

    private static final String CMDS = "gcloud beta emulators firestore start --host-port 0.0.0.0:8080";

    public FirestoreEmulatorContainer(String image) {
        super(image);
        withExposedPorts(8080);
        setWaitStrategy(new LogMessageWaitStrategy()
                .withRegEx("(?s).*running.*$"));
        withCommand("/bin/sh", "-c", parseCmds(CMDS));
    }

    private static String parseCmds(String... cmds) {
        return String.join(" && ", Arrays.asList(cmds));
    }

    public FirestoreEmulatorContainer() {
        this(DEFAULT_GCLOUD_IMAGE);
    }

}
