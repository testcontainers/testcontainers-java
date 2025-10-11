package org.testcontainers.mongodb;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;

/**
 * Testcontainers implementation for MongoDB.
 * <p>
 * Supported images: {@code mongo}, {@code mongodb/mongodb-community-server}, {@code mongodb/mongodb-enterprise-server}
 * <p>
 * Exposed ports: 27017
 */
@Slf4j
public class MongoDBContainer extends GenericContainer<MongoDBContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("mongo");

    private static final DockerImageName COMMUNITY_SERVER_IMAGE = DockerImageName.parse(
        "mongodb/mongodb-community-server"
    );

    private static final DockerImageName ENTERPRISE_SERVER_IMAGE = DockerImageName.parse(
        "mongodb/mongodb-enterprise-server"
    );

    private static final int MONGODB_INTERNAL_PORT = 27017;

    private static final int CONTAINER_EXIT_CODE_OK = 0;

    private static final int AWAIT_INIT_REPLICA_SET_ATTEMPTS = 60;

    private static final String MONGODB_DATABASE_NAME_DEFAULT = "test";

    private static final String STARTER_SCRIPT = "/testcontainers_start.sh";

    private boolean shardingEnabled;

    private boolean rsEnabled;

    public MongoDBContainer(@NonNull String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public MongoDBContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, COMMUNITY_SERVER_IMAGE, ENTERPRISE_SERVER_IMAGE);

        withExposedPorts(MONGODB_INTERNAL_PORT);
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        if (this.shardingEnabled) {
            copyFileToContainer(MountableFile.forClasspathResource("/sharding.sh", 0777), STARTER_SCRIPT);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
        if (this.rsEnabled) {
            initReplicaSet(reused);
        }
    }

    private String[] buildMongoEvalCommand(String command) {
        return new String[] {
            "sh",
            "-c",
            "mongosh mongo --eval \"" + command + "\"  || mongo --eval \"" + command + "\"",
        };
    }

    private void checkMongoNodeExitCode(ExecResult execResult) {
        if (execResult.getExitCode() != CONTAINER_EXIT_CODE_OK) {
            String errorMessage = String.format("An error occurred: %s", execResult.getStdout());
            log.error(errorMessage);
            throw new ReplicaSetInitializationException(errorMessage);
        }
    }

    private String buildMongoWaitCommand() {
        return String.format(
            "var attempt = 0; " +
            "while" +
            "(%s) " +
            "{ " +
            "if (attempt > %d) {quit(1);} " +
            "print('%s ' + attempt); sleep(100);  attempt++; " +
            " }",
            "db.runCommand( { isMaster: 1 } ).ismaster==false",
            AWAIT_INIT_REPLICA_SET_ATTEMPTS,
            "An attempt to await for a single node replica set initialization:"
        );
    }

    private void checkMongoNodeExitCodeAfterWaiting(ExecResult execResultWaitForMaster) {
        if (execResultWaitForMaster.getExitCode() != CONTAINER_EXIT_CODE_OK) {
            String errorMessage = String.format(
                "A single node replica set was not initialized in a set timeout: %d attempts",
                AWAIT_INIT_REPLICA_SET_ATTEMPTS
            );
            log.error(errorMessage);
            throw new ReplicaSetInitializationException(errorMessage);
        }
    }

    @SneakyThrows(value = { IOException.class, InterruptedException.class })
    private void initReplicaSet(boolean reused) {
        if (reused && isReplicationSetAlreadyInitialized()) {
            log.debug("Replica set already initialized.");
        } else {
            log.debug("Initializing a single node node replica set...");
            ExecResult execResultInitRs = execInContainer(buildMongoEvalCommand("rs.initiate();"));
            log.debug(execResultInitRs.getStdout());
            checkMongoNodeExitCode(execResultInitRs);

            log.debug(
                "Awaiting for a single node replica set initialization up to {} attempts",
                AWAIT_INIT_REPLICA_SET_ATTEMPTS
            );
            ExecResult execResultWaitForMaster = execInContainer(buildMongoEvalCommand(buildMongoWaitCommand()));
            log.debug(execResultWaitForMaster.getStdout());

            checkMongoNodeExitCodeAfterWaiting(execResultWaitForMaster);
        }
    }

    public static class ReplicaSetInitializationException extends RuntimeException {

        ReplicaSetInitializationException(String errorMessage) {
            super(errorMessage);
        }
    }

    @SneakyThrows
    private boolean isReplicationSetAlreadyInitialized() {
        // since we are creating a replica set with one node, this node must be primary (state = 1)
        ExecResult execCheckRsInit = execInContainer(
            buildMongoEvalCommand("if(db.adminCommand({replSetGetStatus: 1})['myState'] != 1) quit(900)")
        );
        return execCheckRsInit.getExitCode() == CONTAINER_EXIT_CODE_OK;
    }

    /**
     * Enables replica set on the cluster
     *
     * @return this
     */
    public MongoDBContainer withReplicaSet() {
        this.rsEnabled = true;
        withCommand("--replSet", "docker-rs");
        waitingFor(Wait.forLogMessage("(?i).*waiting for connections.*", 1));
        return this;
    }

    /**
     * Enables sharding on the cluster
     *
     * @return this
     */
    public MongoDBContainer withSharding() {
        this.shardingEnabled = true;
        withCommand("-c", "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT);
        waitingFor(Wait.forLogMessage("(?i).*mongos ready.*", 1));
        withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("sh"));
        return this;
    }

    /**
     * Gets a connection string url, unlike {@link #getReplicaSetUrl} this does not point to a
     * database
     * @return a connection url pointing to a mongodb instance
     */
    public String getConnectionString() {
        return String.format("mongodb://%s:%d", getHost(), getMappedPort(MONGODB_INTERNAL_PORT));
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
    public String getReplicaSetUrl(String databaseName) {
        if (!isRunning()) {
            throw new IllegalStateException("MongoDBContainer should be started first");
        }
        return getConnectionString() + "/" + databaseName;
    }
}
