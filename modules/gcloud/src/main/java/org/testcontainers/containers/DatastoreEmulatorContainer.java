package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;

/**
 * A Datastore container that relies in google cloud sdk.
 *
 * Default port is 8081.
 *
 * @author Eddú Meléndez
 */
public class DatastoreEmulatorContainer extends GCloudGenericContainer<DatastoreEmulatorContainer> {

    private static final String CMD = "gcloud beta emulators datastore start --project test-project --host-port 0.0.0.0:8081";

    public DatastoreEmulatorContainer(String image) {
        super(image);
        withExposedPorts(8081);
        setWaitStrategy(Wait.forHttp("/").forStatusCode(200));
        withCommand("/bin/sh", "-c", CMD);
    }

    public DatastoreEmulatorContainer() {
        this(DEFAULT_GCLOUD_IMAGE);
    }
}
