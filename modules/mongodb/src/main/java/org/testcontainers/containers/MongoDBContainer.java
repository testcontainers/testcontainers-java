package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

/**
 * Constructs a single node MongoDB replica set for testing transactions.
 * <p>To construct a multi-node MongoDB cluster, consider the <a href="https://github.com/silaev/mongodb-replica-set/">mongodb-replica-set project on GitHub</a>
 * <p>Tested on a MongoDB version 4.0.10+ (that is the default version if not specified).
 */
@Slf4j
public class MongoDBContainer extends GenericContainer<MongoDBContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("mongo");
    private static final String DEFAULT_TAG = "4.0.10";
    private static final int CONTAINER_EXIT_CODE_OK = 0;
    private static final int MONGODB_INTERNAL_PORT = 27017;
    private static final int AWAIT_INIT_REPLICA_SET_ATTEMPTS = 60;
    private static final String MONGODB_DATABASE_NAME_DEFAULT = "test";

    /**
     * @deprecated use {@link MongoDBContainer(DockerImageName)} instead
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

        withExposedPorts(MONGODB_INTERNAL_PORT);
        withCommand("--replSet", "docker-rs");
        waitingFor(
            Wait.forLogMessage("(?i).*waiting for connections.*", 1)
        );
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
        return String.format(
            "mongodb://%s:%d/%s",
            getContainerIpAddress(),
            getMappedPort(MONGODB_INTERNAL_PORT),
            databaseName
        );
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        initReplicaSet();
    }

    private String[] buildMongoEvalCommand(final String command) {
        return new String[]{"mongo", "--eval", command};
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

    private void checkMongoNodeExitCodeAfterWaiting(
        final Container.ExecResult execResultWaitForMaster
    ) {
        if (execResultWaitForMaster.getExitCode() != CONTAINER_EXIT_CODE_OK) {
            final String errorMessage = String.format(
                "A single node replica set was not initialized in a set timeout: %d attempts",
                AWAIT_INIT_REPLICA_SET_ATTEMPTS
            );
            log.error(errorMessage);
            throw new ReplicaSetInitializationException(errorMessage);
        }
    }

    @SneakyThrows(value = {IOException.class, InterruptedException.class})
    private void initReplicaSet() {
        log.debug("Initializing a single node node replica set...");
        final ExecResult execResultInitRs = execInContainer(
            buildMongoEvalCommand("rs.initiate();")
        );
        log.debug(execResultInitRs.getStdout());
        checkMongoNodeExitCode(execResultInitRs);

        log.debug(
            "Awaiting for a single node replica set initialization up to {} attempts",
            AWAIT_INIT_REPLICA_SET_ATTEMPTS
        );
        final ExecResult execResultWaitForMaster = execInContainer(
            buildMongoEvalCommand(buildMongoWaitCommand())
        );
        log.debug(execResultWaitForMaster.getStdout());

        checkMongoNodeExitCodeAfterWaiting(execResultWaitForMaster);
    }

    public static class ReplicaSetInitializationException extends RuntimeException {
        ReplicaSetInitializationException(final String errorMessage) {
            super(errorMessage);
        }
    }
}
