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

    private static final String MONGODB_DATABASE_NAME_DEFAULT = "test";

    private static final String DIRECT_CONNECTION = "directConnection=true";

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
        return baseConnectionString() + "/?" + DIRECT_CONNECTION;
    }

    private String baseConnectionString() {
        return String.format("mongodb://%s:%d", getHost(), getMappedPort(MONGODB_INTERNAL_PORT));
    }

    /**
     * Gets a database specific connection string for the default {@value #MONGODB_DATABASE_NAME_DEFAULT} database.
     *
     * @return a database specific connection string.
     */
    public String getDatabaseConnectionString() {
        return getDatabaseConnectionString(MONGODB_DATABASE_NAME_DEFAULT);
    }

    /**
     * Gets a database specific connection string for a provided <code>databaseName</code>.
     *
     * @param databaseName a database name.
     * @return a database specific connection string.
     */
    public String getDatabaseConnectionString(final String databaseName) {
        if (!isRunning()) {
            throw new IllegalStateException("MongoDBContainer should be started first");
        }
        return baseConnectionString() + "/" + databaseName + "?" + DIRECT_CONNECTION;
    }
}
