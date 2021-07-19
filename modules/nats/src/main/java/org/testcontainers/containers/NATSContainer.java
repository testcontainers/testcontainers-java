package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

public class NATSContainer extends GenericContainer<NATSContainer> {

    public static final DockerImageName DEFAULT_NATS_IMAGE = DockerImageName.parse("nats:2.1.9-alpine3.12");

    public static final Integer NATS_PORT = 4222;
    public static final Integer NATS_MGMT_PORT = 8222;

    public NATSContainer() {
        this(DEFAULT_NATS_IMAGE);
    }

    public NATSContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public NATSContainer(DockerImageName dockerImageName) {
        super(dockerImageName);

        addExposedPort(NATS_PORT);
        addExposedPort(NATS_MGMT_PORT);

        this.waitStrategy = new LogMessageWaitStrategy().withRegEx(".*Server is ready.*");
    }
}
