package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;

/**
 * A Datastore container that relies in google cloud sdk. The container provides
 * additional instructions to install the components needed in the alpine images.
 *
 * Default port is 8081.
 *
 * @author Eddú Meléndez
 */
public class DatastoreEmulatorContainer extends GCloudGenericContainer<DatastoreEmulatorContainer> {

	private static final String DATASTORE_EMULATOR_START_COMMAND = "gcloud beta emulators datastore start --project dummy-project --host-port 0.0.0.0:8081";

	private static final String[] CMDS = {"apk --update add openjdk8-jre",
			"gcloud components install beta cloud-datastore-emulator --quiet"};

	public DatastoreEmulatorContainer(String image) {
		super(image, DATASTORE_EMULATOR_START_COMMAND, CMDS);
		withExposedPorts(8081);
		setWaitStrategy(Wait.forHttp("/").forStatusCode(200));
	}

	public DatastoreEmulatorContainer() {
		this(DEFAULT_GCLOUD_IMAGE);
	}
}
