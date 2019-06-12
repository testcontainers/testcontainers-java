package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Represents a MongoDB docker instance
 */
public class MongoContainer<SELF extends MongoContainer<SELF>> extends GenericContainer<SELF> {

    /**
     * MongoDB default port
     */
    public static final int MONGO_PORT = 27017;

    /**
     * MongoDB default image version
     */
    public static final String MONGO_VERSION = "3.6";

    /**
     * MongoDB image name
     */
    public static final String MONGO_IMAGE = "mongo";

    private String admin = "mongo";
    private String adminPassword = "mongo";

    public MongoContainer() {
        this(MONGO_VERSION);
    }

    public MongoContainer(String version) {
        super(MONGO_IMAGE + ":" + version);
        waitingFor(Wait.forLogMessage(".*waiting for connections on port \\d*\n", 1));
    }

    @Override
    protected void configure() {
        addExposedPort(MONGO_PORT);

        addEnv("MONGO_INITDB_ROOT_USERNAME", admin);
        addEnv("MONGO_INITDB_ROOT_PASSWORD", adminPassword);
    }

    /**
     * Configure the admin username
     *
     * @param user the admin username
     * @return the container instance
     */
    public SELF withAdminUser(String user) {
        this.admin = user;
        return self();
    }

    /**
     * Configure the admin password
     *
     * @param password the admin password
     * @return the container instance
     */
    public SELF withAdminPassword(String password) {
        this.adminPassword = password;
        return self();
    }
}
