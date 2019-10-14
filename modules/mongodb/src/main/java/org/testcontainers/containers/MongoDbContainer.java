package org.testcontainers.containers;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;

/**
 * Constructs a single node MongoDB replica set for testing transactions.
 * <p>To construct a multi-node MongoDB cluster, consider the <a href="https://github.com/silaev/mongodb-replica-set/">mongodb-replica-set project on GitHub</a>
 * <p>Tested on a Mongo DB version 4.0.10+ (that is the default version if not specified).
 *
 * @author Konstantin Silaev on 9/30/2019
 */
@Slf4j
public class MongoDbContainer extends GenericContainer<MongoDbContainer> {
    static final int ERROR_CONTAINER_EXIT_CODE = 1;
    static final int MONGODB_INTERNAL_PORT = 27017;
    private static final int AWAIT_INIT_REPLICA_SET_ATTEMPTS = 30;
    private static final String MONGODB_VERSION_DEFAULT = "4.0.10";
    private static final String LOCALHOST = "localhost";
    private static final String MONGODB_DATABASE_NAME_DEFAULT = "test";

    public MongoDbContainer() {
        super("mongo:" + MONGODB_VERSION_DEFAULT);
    }

    public MongoDbContainer(@NonNull final String dockerImageName) {
        super(dockerImageName);
    }

    public String getReplicaSetUrl() {
        if (!isRunning()) {
            throw new IllegalStateException("MongoDbContainer should be started first");
        }
        return String.format(
            "mongodb://%s:%d/%s",
            getContainerIpAddress(),
            getMappedPort(MONGODB_INTERNAL_PORT),
            MONGODB_DATABASE_NAME_DEFAULT
        );
    }

    @Override
    public void start() {
        super.start();
        initReplicaSet();
        logReplicaSetStatus();
    }

    @SneakyThrows(value = {IOException.class, InterruptedException.class})
    private void logReplicaSetStatus() {
        log.debug(
            "REPLICA SET STATUS:\n{}",
            execInContainer(buildMongoEvalCommand("rs.status()")).getStdout()
        );
    }

    @Override
    protected void configure() {
        withExposedPorts(MONGODB_INTERNAL_PORT);
        withCommand("--replSet", "docker-rs");
        waitingFor(
            Wait.forLogMessage(".*waiting for connections on port.*", 1)
        );
    }

    private String getMongoReplicaSetInitializer() {
        final String containerIpAddress = getContainerIpAddress();
        final int containerPort = LOCALHOST.equals(containerIpAddress)
            ? MONGODB_INTERNAL_PORT
            : getMappedPort(MONGODB_INTERNAL_PORT);
        final String initializer = String.format(
            "rs.initiate({\n" +
                "    \"_id\": \"docker-rs\",\n" +
                "    \"members\": [\n" +
                "        {\"_id\": %d, \"host\": \"%s:%d\"}\n    ]\n});",
            0, containerIpAddress, containerPort
        );
        log.debug(initializer);
        return initializer;
    }

    private String[] buildMongoEvalCommand(final String command) {
        return new String[]{"mongo", "--eval", command};
    }

    private void checkMongoNodeExitCode(final Container.ExecResult execResult) {
        if (execResult.getExitCode() == ERROR_CONTAINER_EXIT_CODE) {
            final String errorMessage = String.format("An error occurred: %s", execResult.getStderr());
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
                "print('%s ' + attempt); sleep(1000);  attempt++; " +
                " }",
            "db.runCommand( { isMaster: 1 } ).ismaster==false",
            AWAIT_INIT_REPLICA_SET_ATTEMPTS,
            "An attempt to await for a single node replica set initialization:"
        );
    }

    private void checkMongoNodeExitCodeAfterWaiting(
        final Container.ExecResult execResultWaitForMaster
    ) {
        if (execResultWaitForMaster.getExitCode() == ERROR_CONTAINER_EXIT_CODE) {
            final String errorMessage = String.format(
                "A single node replica set was not initialized in a set timeout: %d attempts",
                AWAIT_INIT_REPLICA_SET_ATTEMPTS
            );
            log.error(errorMessage);
            throw new ReplicaSetInitializationException(errorMessage);
        }
    }

    @SneakyThrows(value = {IOException.class, InterruptedException.class})
    void initReplicaSet() {
        log.debug("Initializing a single node node replica set...");
        final ExecResult execResultInitRs = execInContainer(
            buildMongoEvalCommand(getMongoReplicaSetInitializer())
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
