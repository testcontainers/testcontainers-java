package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

/**
 * A PubSub container that relies in google cloud sdk.
 *
 * Default port is 8085.
 *
 * @author Eddú Meléndez
 */
public class PubSubEmulatorContainer extends GCloudGenericContainer<PubSubEmulatorContainer> {

    private static final String CMD = "gcloud beta emulators pubsub start --host-port 0.0.0.0:8085";

    public PubSubEmulatorContainer(String image) {
        super(image);
        withExposedPorts(8085);
        setWaitStrategy(new LogMessageWaitStrategy()
                .withRegEx("(?s).*started.*$"));
        withCommand("/bin/sh", "-c", CMD);
    }

    public PubSubEmulatorContainer() {
        this(DEFAULT_GCLOUD_IMAGE);
    }
}
