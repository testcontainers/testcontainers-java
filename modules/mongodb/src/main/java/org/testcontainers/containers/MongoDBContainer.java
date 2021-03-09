package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static final String DOCKER_ENTRYPOINT_INIT_DIR = "docker-entrypoint-initdb.d";

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
        checkIfRunning();
        return String.format(
            "mongodb://%s:%d/%s",
            getContainerIpAddress(),
            getMappedPort(MONGODB_INTERNAL_PORT),
            databaseName
        );
    }

    private void checkIfRunning() {
        if (!isRunning()) {
            throw new IllegalStateException("MongoDBContainer should be started first");
        }
    }

    /**
     * Loads and executes JavaScript files directly.
     * Note that all the files are supposed to be delivered to a container via proper commands before starting.
     * Should be used as an alternative to docker-entrypoint-initdb.d. This is because at docker-entrypoint-initdb.d
     * stage, a replica set is not yet initialized and thus cannot accept operations.
     *
     * @param paths directory or file names as an array of strings.
     * @see GenericContainer#withClasspathResourceMapping(String, String, BindMode) etc.
     */
    @SneakyThrows(value = {IOException.class, InterruptedException.class})
    public void loadAndExecuteJsFiles(final String... paths) {
        checkIfRunning();
        final String loadCommand =
            Stream.of(paths).map(it -> "load(\"" + it + "\")").collect(Collectors.joining(";"));
        final ExecResult execResult = execInContainer(buildMongoEvalCommand(loadCommand));
        if (execResult.getExitCode() != CONTAINER_EXIT_CODE_OK) {
            final String errorMessage = String.format("An error occurred: %s", execResult.getStdout());
            log.error(errorMessage);
            throw new LoadAndExecuteJsFilesException(errorMessage);
        }
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

    @SneakyThrows(value = {IOException.class, InterruptedException.class})
    private void checkDockerEntrypointDirIsEmpty() {
        final ExecResult execResult = execInContainer(
            "/bin/bash",
            "-c",
            String.format(
                "if [ -n \"$(find \"%s\" -maxdepth 0 -type d -empty 2>/dev/null)\" ]; then exit 0; else exit -1; fi",
                DOCKER_ENTRYPOINT_INIT_DIR
            )
        );
        if (execResult.getExitCode() != CONTAINER_EXIT_CODE_OK) {
            throw new DockerEntrypointInitDirIsNotEmptyException(
                String.format(
                    "%s is supposed to be empty while running with the --replSet command-line option. " +
                        "Consider using loadAndExecuteJsFiles(...). Error: %s",
                    DOCKER_ENTRYPOINT_INIT_DIR,
                    execResult.getStdout()
                )
            );
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
        checkDockerEntrypointDirIsEmpty();
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

    public static class DockerEntrypointInitDirIsNotEmptyException extends RuntimeException {
        DockerEntrypointInitDirIsNotEmptyException(final String errorMessage) {
            super(errorMessage);
        }
    }

    public static class LoadAndExecuteJsFilesException extends RuntimeException {
        LoadAndExecuteJsFilesException(final String errorMessage) {
            super(errorMessage);
        }
    }
}
