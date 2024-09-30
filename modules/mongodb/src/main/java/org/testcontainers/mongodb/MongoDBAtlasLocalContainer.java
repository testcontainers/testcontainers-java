package org.testcontainers.mongodb;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for MongoDB Atlas.
 * <p>
 * Supported images: {@code mongodb/mongodb-atlas-local}
 * <p>
 * Exposed ports: 27017
 */
public class MongoDBAtlasLocalContainer extends GenericContainer<MongoDBAtlasLocalContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("mongodb/mongodb-atlas-local");

    private static final int MONGODB_INTERNAL_PORT = 27017;

    public MongoDBAtlasLocalContainer(final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public MongoDBAtlasLocalContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(MONGODB_INTERNAL_PORT);
        waitingFor(Wait.forSuccessfulCommand("runner healthcheck"));
    }

    /**
     * Get the connection string to MongoDB.
     */
    public String getConnectionString() {
        return String.format("mongodb://%s:%d/?directConnection=true", getHost(), getMappedPort(MONGODB_INTERNAL_PORT));
    }
}
