package org.testcontainers.containers;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

public class NATSContainer extends GenericContainer<NATSContainer>{

    public static final DockerImageName DEFAULT_NATS_IMAGE = DockerImageName.parse("nats:2.1.9-alpine3.12");

    public static final String NATS_PROTOCOL = "nats://";
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

        addFixedExposedPort(NATS_PORT, NATS_PORT);
        addFixedExposedPort(NATS_MGMT_PORT, NATS_MGMT_PORT);
    }

    public static Connection getConnection(ContainerState containerState) {

        Options options = new Options.Builder().
            server(NATS_PROTOCOL + containerState.getHost() + ":" + NATS_PORT).
            build();
        Connection connection = null;
        try {
            connection = Nats.connect(options);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return connection;
    }
}
