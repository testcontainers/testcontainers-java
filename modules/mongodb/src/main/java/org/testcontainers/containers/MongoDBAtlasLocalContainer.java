package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class MongoDBAtlasLocalContainer extends GenericContainer<MongoDBAtlasLocalContainer> {

    public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("mongodb/mongodb-atlas-local");
    public static final int MONGODB_INTERNAL_PORT = 27017;

    public MongoDBAtlasLocalContainer(final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public MongoDBAtlasLocalContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    }

    MongoDBAtlasContainerDef createContainerDef() {
        return new MongoDBAtlasContainerDef();
    }

    /**
     * Create a custom container definition with the following properties:
     * - Expose the internal MongoDB port (27017)
     * - Set a wait strategy to wait for the "runner healthcheck" command to succeed (this is a custom command that is run by the MongoDB Atlas Local container identical to the health check baked into the original container, but without the 30s interval that makes startup time interminably slow)
     */
    private static class MongoDBAtlasContainerDef extends ContainerDef {

        MongoDBAtlasContainerDef() {
            this.addExposedTcpPort(MONGODB_INTERNAL_PORT);
            this.setWaitStrategy(Wait.forSuccessfulCommand("runner healthcheck"));
        }
    }

    /**
     * Get the connection string to MongoDB.
     */
    public String getConnectionString() {
        return String.format("mongodb://%s:%d", this.getHost(), this.getMappedPort(MONGODB_INTERNAL_PORT));
    }
}
