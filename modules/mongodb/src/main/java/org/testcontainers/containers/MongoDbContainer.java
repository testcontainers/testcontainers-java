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
    private static final int CONTAINER_EXIT_CODE_OK = 0;
    static final int MONGODB_INTERNAL_PORT = 27017;
    private static final int AWAIT_INIT_REPLICA_SET_ATTEMPTS = 30;
    private static final String MONGODB_VERSION_DEFAULT = "4.0.10";
    private static final String LOCALHOST = "localhost";
    private static final String MONGODB_DATABASE_NAME_DEFAULT = "test";

    public MongoDbContainer() {
        super("mongo:" + MONGODB_VERSION_DEFAULT);
        configureMongoDbContainer();

    }

    public MongoDbContainer(@NonNull final String dockerImageName) {
        super(dockerImageName);
        configureMongoDbContainer();
    }

    private void configureMongoDbContainer() {
        withExposedPorts(MONGODB_INTERNAL_PORT);
        withCommand("--replSet", "docker-rs");
        waitingFor(
            Wait.forLogMessage(".*waiting for connections on port.*", 1)
        );
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

    /**
     * Gets a string to initialize MongoDB replica set.
     *
     * The following explains why LOCALHOST is used here.
     * When it comes to a Single node replica set, it requires a proper
     * port setting depending on an environment.
     * The table below shows an example demonstrating such specific in detail:
     *
     * <blockquote>
     * <table class="striped">
     * <thead>
     *     <tr>
     *         <th scope="col" style="text-align:center">Difference
     *         <th scope="col" style="text-align:center">local Docker host example
     *         <th scope="col" style="text-align:center">local Docker host running tests from inside a container with mapping the Docker socket or <br> remote Docker daemon
     * </thead>
     * <tbody>
     *     <tr>
     *         <th scope="row" style="text-align:center"><code>a host string to initialize a replica set</code>
     *         <td style="text-align:center">localhost:27017 <br> Despite the fact that Docker allocates 33538 (for instance) as a random port for a container
     *         <td style="text-align:center">172.17.0.1:33542
     *     <tr>
     *     <tr>
     *         <th scope="row" style="text-align:center"><code>a url to use with a Java Mongo driver </code>
     *         <td style="text-align:center">mongodb://localhost:33538/test
     *         <td style="text-align:center">mongodb://172.17.0.1:33542/test
     *     <tr>
     * </tbody>
     * </table>
     * </blockquote>
     *
     * @return String to initialize MongoDB replica set
     */
    private String getMongoReplicaSetInitializer() {
        final String containerIpAddress = getContainerIpAddress();
        final int containerPort = LOCALHOST.equalsIgnoreCase(containerIpAddress)
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
