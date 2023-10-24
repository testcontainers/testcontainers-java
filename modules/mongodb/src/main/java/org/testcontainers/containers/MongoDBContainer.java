package org.testcontainers.containers;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;

/**
 * Testcontainers implementation for MongoDB.
 * <p>
 * Supported image: {@code mongo}
 * <p>
 * Exposed ports: 27017
 */
@Slf4j
public class MongoDBContainer extends GenericContainer<MongoDBContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("mongo");

    private static final String DEFAULT_TAG = "4.0.10";

    private static final int CONTAINER_EXIT_CODE_OK = 0;

    private static final int MONGODB_INTERNAL_PORT = 27017;

    private static final int AWAIT_INIT_REPLICA_SET_ATTEMPTS = 60;

    private static final String MONGODB_DATABASE_NAME_DEFAULT = "test";

    private static final String STARTER_SCRIPT = "/testcontainers_start.sh";

    private final MongoDBContainerDef containerDef = new MongoDBContainerDef();

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
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        addExposedPort(MONGODB_INTERNAL_PORT);
    }

    @Override
    public void configure() {
        setCommand(this.containerDef.getCommand());
        setWaitStrategy(this.containerDef.getWaitStrategy());
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        if (this.containerDef.isShardingEnabled()) {
            copyFileToContainer(MountableFile.forClasspathResource("/sharding.sh", 0777), STARTER_SCRIPT);
        }
    }

    /**
     * Enables sharding on the cluster
     *
     * @return this
     */
    public MongoDBContainer withSharding() {
        this.containerDef.withSharding();
        return this;
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
        if (!this.containerDef.isShardingEnabled()) {
            initReplicaSet(reused);
        }
    }

    /**
     * Gets a connection string url, unlike {@link #getReplicaSetUrl} this does point to a
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
    public String getReplicaSetUrl(final String databaseName) {
        if (!isRunning()) {
            throw new IllegalStateException("MongoDBContainer should be started first");
        }
        return getConnectionString() + "/" + databaseName;
    }

    private String[] buildMongoEvalCommand(final String command) {
        return new String[] {
            "sh",
            "-c",
            "mongosh mongo --eval \"" + command + "\"  || mongo --eval \"" + command + "\"",
        };
    }

    private void checkMongoNodeExitCode(final Container.ExecResult execResult) {
        if (execResult.getExitCode() != CONTAINER_EXIT_CODE_OK) {
            final String errorMessage = String.format("An error occurred: %s", execResult.getStdout());
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

    private void checkMongoNodeExitCodeAfterWaiting(final Container.ExecResult execResultWaitForMaster) {
        if (execResultWaitForMaster.getExitCode() != CONTAINER_EXIT_CODE_OK) {
            final String errorMessage = String.format(
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
            final ExecResult execResultInitRs = execInContainer(buildMongoEvalCommand("rs.initiate();"));
            log.debug(execResultInitRs.getStdout());
            checkMongoNodeExitCode(execResultInitRs);

            log.debug(
                "Awaiting for a single node replica set initialization up to {} attempts",
                AWAIT_INIT_REPLICA_SET_ATTEMPTS
            );
            final ExecResult execResultWaitForMaster = execInContainer(buildMongoEvalCommand(buildMongoWaitCommand()));
            log.debug(execResultWaitForMaster.getStdout());

            checkMongoNodeExitCodeAfterWaiting(execResultWaitForMaster);
        }
    }

    public static class ReplicaSetInitializationException extends RuntimeException {

        ReplicaSetInitializationException(final String errorMessage) {
            super(errorMessage);
        }
    }

    @SneakyThrows
    private boolean isReplicationSetAlreadyInitialized() {
        // since we are creating a replica set with one node, this node must be primary (state = 1)
        final ExecResult execCheckRsInit = execInContainer(
            buildMongoEvalCommand("if(db.adminCommand({replSetGetStatus: 1})['myState'] != 1) quit(900)")
        );
        return execCheckRsInit.getExitCode() == CONTAINER_EXIT_CODE_OK;
    }

    private static class MongoDBContainerDef extends ContainerDef {

        @Getter
        private boolean shardingEnabled;

        @Getter
        private WaitStrategy waitStrategy;

        MongoDBContainerDef() {
            addExposedTcpPort(MONGODB_INTERNAL_PORT);
            setCommand("--replSet", "docker-rs");
            this.waitStrategy = Wait.forLogMessage("(?i).*waiting for connections.*", 1);
        }

        void withSharding() {
            this.shardingEnabled = true;
            setCommand("sh", "-c", "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT);
            this.waitStrategy = Wait.forLogMessage("(?i).*mongos ready.*", 1);
        }

        // FIXME: applyTo is not called and sh should be removed from command
        @Override
        public void applyTo(CreateContainerCmd createCommand) {
            super.applyTo(createCommand);

            if (this.shardingEnabled) {
                createCommand.withEntrypoint("sh");
            }
        }
    }
}
