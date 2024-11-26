package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * A Spanner container. Default ports: 9010 for GRPC and 9020 for HTTP.
 * <p>
 * Supported image: {@code gcr.io/cloud-spanner-emulator/emulator}
 */
public class SpannerEmulatorContainer extends GenericContainer<SpannerEmulatorContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "gcr.io/cloud-spanner-emulator/emulator"
    );

    private static final int GRPC_PORT = 9010;

    private static final int HTTP_PORT = 9020;

    public SpannerEmulatorContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public SpannerEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(GRPC_PORT, HTTP_PORT);
        setWaitStrategy(Wait.forLogMessage(".*Cloud Spanner emulator running\\..*", 1));
    }

    /**
     * @return a <code>host:port</code> pair corresponding to the address on which the emulator's
     * gRPC endpoint is reachable from the test host machine. Directly usable as a parameter to the
     * com.google.cloud.spanner.SpannerOptions.Builder#setEmulatorHost(java.lang.String) method.
     */
    public String getEmulatorGrpcEndpoint() {
        return getHost() + ":" + getMappedPort(GRPC_PORT);
    }

    /**
     * @return a <code>host:port</code> pair corresponding to the address on which the emulator's
     * HTTP REST endpoint is reachable from the test host machine.
     */
    public String getEmulatorHttpEndpoint() {
        return getHost() + ":" + getMappedPort(HTTP_PORT);
    }
}
