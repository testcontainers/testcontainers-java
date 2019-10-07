package org.testcontainers.containers;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;

/**
 * Constructs a single node MongoDB replica set for testing transactions.
 * <p>To construct a multi-node MongoDB cluster, consider <a href="https://github.com/silaev/mongodb-replica-set/">mongodb-replica-set project on GitHub</a>
 *
 * <blockquote>
 * <table class="striped">
 * <caption style="display:none">Chart shows a pattern for local and remote Docker support</caption>
 * <thead>
 *     <tr>
 *         <th scope="col" style="text-align:center">local docker host
 *         <th scope="col" style="text-align:center">local docker host running tests from inside a container with mapping the Docker socket
 *         <th scope="col" style="text-align:center">remote docker daemon
 * </thead>
 * <tbody>
 *     <tr>
 *         <td style="text-align:center">+
 *         <td style="text-align:center">+
 *         <td style="text-align:center">+
 *     <tr>
 * </tbody>
 * </table>
 * </blockquote>
 * <p>
 * Tested on a Mongo DB version 4.0.10 (that is the default version if not specified) and up.
 *
 * <h3>Example usage (note that the MongoDbContainer is test framework agnostic)</h3>
 * <p>The example of a JUnit5 test class:
 * <pre style="code">
 * import org.junit.jupiter.api.AfterEach;
 * import org.junit.jupiter.api.BeforeEach;
 * import org.junit.jupiter.api.Test;
 * import org.testcontainers.mongodb.MongoDbContainer;
 *
 * import static org.junit.jupiter.api.Assertions.assertNotNull;
 *
 * class ITTest {
 *     private final MongoDbContainer mongoDbContainer = new MongoDbContainer(
 *         //"mongo:4.2.0"
 *     );
 *
 *     {@literal @}BeforeEach
 *     void setUp() {
 *         mongoDbContainer.start();
 *     }
 *
 *     {@literal @}AfterEach
 *     void tearDown() {
 *         mongoDbContainer.stop();
 *     }
 *
 *     {@literal @}Test
 *     void shouldTestReplicaSetUrl() {
 *         assertNotNull(mongoDbContainer.getReplicaSetUrl());
 *     }
 * }
 * </pre>
 *
 * <p>The example of a SpringBoot+SpringData test with JUnit5:
 * <pre style="code">
 * import org.junit.jupiter.api.AfterAll;
 * import org.junit.jupiter.api.BeforeAll;
 * import org.junit.jupiter.api.Test;
 * import org.springframework.boot.test.context.SpringBootTest;
 * import org.springframework.boot.test.util.TestPropertyValues;
 * import org.springframework.context.ApplicationContextInitializer;
 * import org.springframework.context.ConfigurableApplicationContext;
 * import org.springframework.test.context.ContextConfiguration;
 * import org.testcontainers.mongodb.MongoDbContainer;
 *
 * import static org.junit.jupiter.api.Assertions.assertNotNull;
 *
 * {@literal @}SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * //@DataMongoTest
 * {@literal @}ContextConfiguration(initializers = ITTest.Initializer.class)
 * class ITTest {
 *     private static final MongoDbContainer MONGO_DB_CONTAINER = new MongoDbContainer(
 *             //"mongo:4.2.0"
 *     );
 *
 *     {@literal @}BeforeAll
 *     static void setUp() {
 *         MONGO_DB_CONTAINER.start();
 *     }
 *
 *     {@literal @}AfterAll
 *     static void tearDown() {
 *         MONGO_DB_CONTAINER.stop();
 *     }
 *
 *     {@literal @}Test
 *     void shouldTestReplicaSetUrl() {
 *         assertNotNull(MONGO_DB_CONTAINER.getReplicaSetUrl());
 *     }
 *
 *     static class Initializer implements ApplicationContextInitializer&#60;ConfigurableApplicationContext&#62; {
 *        {@literal @}@Override
 *         public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
 *             TestPropertyValues.of(
 *                     String.format("spring.data.mongodb.uri: %s", MONGO_DB_CONTAINER.getReplicaSetUrl())
 *             ).applyTo(configurableApplicationContext);
 *         }
 *     }
 * }
 * </pre>
 *
 * @author Konstantin Silaev on 9/30/2019
 */
@Slf4j
public class MongoDbContainer extends GenericContainer<MongoDbContainer> {
    static final int ERROR_CONTAINER_EXIT_CODE = 1;
    static final int MONGO_DB_INTERNAL_PORT = 27017;
    private static final int AWAIT_INIT_REPLICA_SET_ATTEMPTS = 30;
    private static final String MONGODB_VERSION_DEFAULT = "4.0.10";
    private static final String LOCALHOST = "localhost";

    public MongoDbContainer() {
        super("mongo:" + MONGODB_VERSION_DEFAULT);
    }

    public MongoDbContainer(@NonNull final String dockerImageName) {
        super(dockerImageName);
    }

    public String getReplicaSetUrl() {
        if (!isRunning()) {
            throw new IllegalStateException(
                String.format(
                    "Please, start %s first", MongoDbContainer.class.getCanonicalName())
            );
        }
        return String.format(
            "mongodb://%s:%d/test",
            getContainerIpAddress(), getMappedPort(MONGO_DB_INTERNAL_PORT)
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
        withExposedPorts(MONGO_DB_INTERNAL_PORT);
        withCommand("--replSet", "docker-rs");
        waitingFor(
            Wait.forLogMessage(".*waiting for connections on port.*", 1)
        );
    }

    private String getMongoReplicaSetInitializer() {
        val containerIpAddress = getContainerIpAddress();
        val containerPort = LOCALHOST.equals(containerIpAddress)
            ? MONGO_DB_INTERNAL_PORT
            : getMappedPort(MONGO_DB_INTERNAL_PORT);
        val initializer = String.format(
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
            val errorMessage = String.format("An error occurred: %s", execResult.getStderr());
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
            val errorMessage = String.format(
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
        val execResultInitRs = execInContainer(
            buildMongoEvalCommand(getMongoReplicaSetInitializer())
        );
        log.debug(execResultInitRs.getStdout());
        checkMongoNodeExitCode(execResultInitRs);

        log.debug(
            "Awaiting for a single node replica set initialization up to {} attempts",
            AWAIT_INIT_REPLICA_SET_ATTEMPTS
        );
        val execResultWaitForMaster = execInContainer(
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
