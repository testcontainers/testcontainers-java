package org.testcontainers.containers;

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

	private static final String PUBSUB_EMULATOR_START_COMMAND = "gcloud beta emulators pubsub start --host-port 0.0.0.0:8085";

	private static final String[] CMDS = {"apk --update add openjdk7-jre",
			"gcloud components install beta pubsub-emulator --quiet"};

	public PubSubEmulatorContainer(String image) {
		super(image, PUBSUB_EMULATOR_START_COMMAND, CMDS);
		withExposedPorts(8085);
		setWaitStrategy(new LogMessageWaitStrategy()
				.withRegEx("(?s).*started.*$"));
	}

	public PubSubEmulatorContainer() {
		this(DEFAULT_GCLOUD_IMAGE);
	}
}
