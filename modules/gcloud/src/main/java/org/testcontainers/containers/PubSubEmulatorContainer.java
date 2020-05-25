package org.testcontainers.containers;

import java.time.Duration;
import java.util.Arrays;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

/**
 * A PubSub container that relies in google cloud sdk. The container provides
 * additional instructions to install the components needed in the alpine images.
 *
 * Default port is 8085.
 *
 * @author Eddú Meléndez
 */
public class PubSubEmulatorContainer extends GCloudGenericContainer<PubSubEmulatorContainer> {

    private static final String[] CMDS = {"apk --update add openjdk7-jre",
            "gcloud components install beta pubsub-emulator --quiet",
            "gcloud beta emulators pubsub start --host-port 0.0.0.0:8085"};

    public PubSubEmulatorContainer(String image) {
        super(image);
        withExposedPorts(8085);
        setWaitStrategy(new LogMessageWaitStrategy()
                .withRegEx("(?s).*started.*$")
                .withStartupTimeout(Duration.ofSeconds(120)));
        withCommand("/bin/sh", "-c", parseCmds(CMDS));
    }

    private static String parseCmds(String... cmds) {
        return String.join(" && ", Arrays.asList(cmds));
    }

    public PubSubEmulatorContainer() {
        this(DEFAULT_GCLOUD_IMAGE);
    }
}
