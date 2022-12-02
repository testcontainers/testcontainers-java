package org.testcontainers.gcloud;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * A Datastore container that relies in google cloud sdk.
 *
 * Default port is 8081.
 *
 * @author Eddú Meléndez
 */
public class DatastoreEmulatorContainer extends GenericContainer<DatastoreEmulatorContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "gcr.io/google.com/cloudsdktool/cloud-sdk"
    );

    private static final String CMD =
        "gcloud beta emulators datastore start --project test-project --host-port 0.0.0.0:8081";

    private static final int HTTP_PORT = 8081;

    private String flags;

    public DatastoreEmulatorContainer(final String image) {
        this(DockerImageName.parse(image));
    }

    public DatastoreEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(HTTP_PORT);
        setWaitStrategy(Wait.forHttp("/").forStatusCode(200));
    }

    @Override
    protected void configure() {
        String command = CMD;
        if (this.flags != null && !this.flags.isEmpty()) {
            command += " " + this.flags;
        }
        withCommand("/bin/sh", "-c", command);
    }

    public DatastoreEmulatorContainer withFlags(String flags) {
        this.flags = flags;
        return this;
    }

    /**
     * @return a <code>host:port</code> pair corresponding to the address on which the emulator is
     * reachable from the test host machine. Directly usable as a parameter to the
     * com.google.cloud.ServiceOptions.Builder#setHost(java.lang.String) method.
     */
    public String getEmulatorEndpoint() {
        return getHost() + ":" + getMappedPort(HTTP_PORT);
    }
}
