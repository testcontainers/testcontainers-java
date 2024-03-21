package org.testcontainers.containers;

import lombok.NonNull;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for MongoDB.
 * <p>
 * Supported images: {@code mongo}, {@code mongodb/mongodb-community-server}, {@code mongodb/mongodb-enterprise-server}
 * <p>
 * Exposed ports: 27017
 */
public class MongoDBContainer extends GenericContainer<MongoDBContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("mongo");

    private static final DockerImageName COMMUNITY_SERVER_IMAGE = DockerImageName.parse(
        "mongodb/mongodb-community-server"
    );

    private static final DockerImageName ENTERPRISE_SERVER_IMAGE = DockerImageName.parse(
        "mongodb/mongodb-enterprise-server"
    );

    private static final String DEFAULT_TAG = "4.0.10";

    private static final String MONGODB_DATABASE_NAME_DEFAULT = "test";

    /**
     * @deprecated use {@link #MongoDBContainer(DockerImageName)} instead
     */
    @Deprecated
    public MongoDBContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public MongoDBContainer(@NonNull final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public MongoDBContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, COMMUNITY_SERVER_IMAGE, ENTERPRISE_SERVER_IMAGE);
    }

    @Override
    MongoDBContainerDef createContainerDef() {
        return new MongoDBContainerDef();
    }

    @Override
    MongoDBContainerDef getContainerDef() {
        return (MongoDBContainerDef) super.getContainerDef();
    }

    @Override
    StartedMongoDBContainer getStartedContainer() {
        return (StartedMongoDBContainer) super.getStartedContainer();
    }

    /**
     * Enables sharding on the cluster
     *
     * @return this
     */
    public MongoDBContainer withSharding() {
        getContainerDef().withSharding();
        return this;
    }

    /**
     * Gets a connection string url, unlike {@link #getReplicaSetUrl} this does not point to a
     * database
     * @return a connection url pointing to a mongodb instance
     */
    public String getConnectionString() {
        return getStartedContainer().getConnectionString();
    }

    /**
     * Gets a replica set url for the default {@value #MONGODB_DATABASE_NAME_DEFAULT} database.
     *
     * @return a replica set url.
     */
    public String getReplicaSetUrl() {
        return getReplicaSetUrl(MONGODB_DATABASE_NAME_DEFAULT);
    }

    /**
     * Gets a replica set url for a provided <code>databaseName</code>.
     *
     * @param databaseName a database name.
     * @return a replica set url.
     */
    public String getReplicaSetUrl(final String databaseName) {
        if (!isRunning()) {
            throw new IllegalStateException("MongoDBContainer should be started first");
        }
        return getConnectionString() + "/" + databaseName;
    }
}
