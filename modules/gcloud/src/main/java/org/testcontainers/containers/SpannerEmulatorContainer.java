package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

/**
 * A Spanner container. Default ports: 9010 for GRPC and 9020 for HTTP.
 *
 * @author Eddú Meléndez
 */
public class SpannerEmulatorContainer extends GenericContainer<SpannerEmulatorContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("gcr.io/cloud-spanner-emulator/emulator");

    private static final int GRPC_PORT = 9010;
    private static final int HTTP_PORT = 9020;

    public SpannerEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(GRPC_PORT, HTTP_PORT);
        setWaitStrategy(new LogMessageWaitStrategy()
                .withRegEx(".*Cloud Spanner emulator running\\..*"));
    }

}
