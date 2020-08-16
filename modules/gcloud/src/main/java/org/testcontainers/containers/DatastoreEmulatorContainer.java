package org.testcontainers.containers;

import java.util.Arrays;

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

    private static final String CMDS = "gcloud beta emulators datastore start --project dummy-project --host-port 0.0.0.0:8081";

    public DatastoreEmulatorContainer(String image) {
        super(image);
        withExposedPorts(8081);
        setWaitStrategy(Wait.forHttp("/").forStatusCode(200));
        withCommand("/bin/sh", "-c", parseCmds(CMDS));
    }

    private static String parseCmds(String... cmds) {
        return String.join(" && ", Arrays.asList(cmds));
    }

    public DatastoreEmulatorContainer() {
        this(DEFAULT_GCLOUD_IMAGE);
    }
}
