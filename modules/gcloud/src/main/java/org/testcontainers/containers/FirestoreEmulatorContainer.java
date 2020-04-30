package org.testcontainers.containers;

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

	private static final String FIRESTORE_EMULATOR_START_COMMAND = "gcloud beta emulators firestore start --host-port 0.0.0.0:8080";

	private static final String[] CMDS = {"apk --update add openjdk8-jre",
			"gcloud components install beta cloud-firestore-emulator --quiet"};

	public FirestoreEmulatorContainer(String image) {
		super(image, FIRESTORE_EMULATOR_START_COMMAND, CMDS);
		withExposedPorts(8080);
		setWaitStrategy(new LogMessageWaitStrategy()
				.withRegEx("(?s).*running.*$"));
	}

	public FirestoreEmulatorContainer() {
		this(DEFAULT_GCLOUD_IMAGE);
	}

}
