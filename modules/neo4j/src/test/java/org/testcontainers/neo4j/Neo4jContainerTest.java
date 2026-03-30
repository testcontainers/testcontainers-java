package org.testcontainers.neo4j;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.utility.DockerLoggerFactory;
import org.testcontainers.utility.MountableFile;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assumptions.assumeThat;

class Neo4jContainerTest {

    // See org.testcontainers.utility.LicenseAcceptance#ACCEPTANCE_FILE_NAME
    private static final String ACCEPTANCE_FILE_LOCATION = "/container-license-acceptance.txt";

    @Test
    void authenticated() {
        try (
            // container {
            Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4")
            // }
        ) {
            neo4j.start();
            try (Driver driver = getDriver(neo4j); Session session = driver.session()) {
                long one = session.run("RETURN 1", Collections.emptyMap()).next().get(0).asLong();
                assertThat(one).isEqualTo(1L);
            }
        }
    }

    @Test
    void shouldDisableAuthentication() {
        try (
            // spotless:off
            // withoutAuthentication {
            Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4")
                .withoutAuthentication()
            // }
            // spotless:on
        ) {
            neo4j.start();
            try (Driver driver = getDriver(neo4j); Session session = driver.session()) {
                long one = session.run("RETURN 1", Collections.emptyMap()).next().get(0).asLong();
                assertThat(one).isEqualTo(1L);
            }
        }
    }

    @Test
    void shouldCopyDatabase() {
        // no aarch64 image available for Neo4j 3.5
        assumeThat(DockerClientFactory.instance().getInfo().getArchitecture()).isNotEqualTo("aarch64");
        try (
            // copyDatabase {
            Neo4jContainer neo4j = new Neo4jContainer("neo4j:3.5.30")
                .withDatabase(MountableFile.forClasspathResource("/test-graph.db"))
            // }
        ) {
            neo4j.start();
            try (Driver driver = getDriver(neo4j); Session session = driver.session()) {
                Result result = session.run("MATCH (t:Thing) RETURN t");
                assertThat(result.list().stream().map(r -> r.get("t").get("name").asString()))
                    .containsExactlyInAnyOrder("Thing", "Thing 2", "Thing 3", "A box");
            }
        }
    }

    @Test
    void shouldFailOnCopyDatabaseForDefaultNeo4j4Image() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> {
                new Neo4jContainer("neo4j:4.4.1").withDatabase(MountableFile.forClasspathResource("/test-graph.db"));
            })
            .withMessage("Copying database folder is not supported for Neo4j instances with version 4.0 or higher.");
    }

    @Test
    void shouldFailOnCopyDatabaseForCustomNeo4j4Image() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> {
                new Neo4jContainer("neo4j:4.4.1").withDatabase(MountableFile.forClasspathResource("/test-graph.db"));
            })
            .withMessage("Copying database folder is not supported for Neo4j instances with version 4.0 or higher.");
    }

    @Test
    void shouldFailOnCopyDatabaseForCustomNonSemverNeo4j4Image() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> {
                new Neo4jContainer("neo4j:latest").withDatabase(MountableFile.forClasspathResource("/test-graph.db"));
            })
            .withMessage("Copying database folder is not supported for Neo4j instances with version 4.0 or higher.");
    }

    @Test
    void shouldCopyPlugins() {
        try (
            // registerPluginsPath {
            Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4")
                .withPlugins(MountableFile.forClasspathResource("/custom-plugins"))
            // }
        ) {
            neo4j.start();
            try (Driver driver = getDriver(neo4j); Session session = driver.session()) {
                assertThatCustomPluginWasCopied(session);
            }
        }
    }

    @Test
    void shouldCopyPlugin() {
        try (
            // registerPluginsJar {
            Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4")
                .withPlugins(MountableFile.forClasspathResource("/custom-plugins/hello-world.jar"))
            // }
        ) {
            neo4j.start();
            try (Driver driver = getDriver(neo4j); Session session = driver.session()) {
                assertThatCustomPluginWasCopied(session);
            }
        }
    }

    private static void assertThatCustomPluginWasCopied(Session session) {
        Result result = session.run("RETURN ac.simons.helloWorld('Testcontainers') AS greeting");
        Record singleRecord = result.single();
        assertThat(singleRecord).isNotNull();
        assertThat(singleRecord.get("greeting").asString()).isEqualTo("Hello, Testcontainers");
    }

    @Test
    void shouldRunEnterprise() {
        assumeThat(Neo4jContainerTest.class.getResource(ACCEPTANCE_FILE_LOCATION)).isNotNull();

        try (
            // enterpriseEdition {
            Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4-enterprise")
                .acceptLicense()
                // }
                .withAdminPassword("Picard123")
        ) {
            neo4j.start();
            try (Driver driver = getDriver(neo4j); Session session = driver.session()) {
                String edition = session
                    .run("CALL dbms.components() YIELD edition RETURN edition", Collections.emptyMap())
                    .next()
                    .get(0)
                    .asString();
                assertThat(edition).isEqualTo("enterprise");
            }
        }
    }

    @Test
    void shouldAddConfigToEnvironment() {
        // neo4jConfiguration {
        Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4")
            .withNeo4jConfig("dbms.security.procedures.unrestricted", "apoc.*,algo.*")
            .withNeo4jConfig("dbms.tx_log.rotation.size", "42M");
        // }

        assertThat(neo4j.getEnvMap()).containsEntry("NEO4J_dbms_security_procedures_unrestricted", "apoc.*,algo.*");
        assertThat(neo4j.getEnvMap()).containsEntry("NEO4J_dbms_tx__log_rotation_size", "42M");
    }

    @Test
    void shouldRespectEnvironmentAuth() {
        Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4").withEnv("NEO4J_AUTH", "neo4j/secret");

        neo4j.configure();

        assertThat(neo4j.getEnvMap()).containsEntry("NEO4J_AUTH", "neo4j/secret");
    }

    @Test
    void shouldSetCustomPasswordCorrectly() {
        // withAdminPassword {
        Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4").withAdminPassword("verySecret");
        // }

        neo4j.configure();

        assertThat(neo4j.getEnvMap()).containsEntry("NEO4J_AUTH", "neo4j/verySecret");
    }

    @Test
    void adminPasswordOverrulesEnvironmentAuth() {
        Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4")
            .withEnv("NEO4J_AUTH", "neo4j/secret")
            .withAdminPassword("anotherSecret");

        neo4j.configure();

        assertThat(neo4j.getEnvMap()).containsEntry("NEO4J_AUTH", "neo4j/anotherSecret");
    }

    @Test
    void shouldWithoutAuthenticationOverrulesEnvironmentAuth() {
        Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4")
            .withEnv("NEO4J_AUTH", "neo4j/secret")
            .withoutAuthentication();

        neo4j.configure();

        assertThat(neo4j.getEnvMap()).containsEntry("NEO4J_AUTH", "none");
    }

    @Test
    void shouldRespectAlreadyDefinedPortMappingsBolt() {
        Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4").withExposedPorts(7687);

        neo4j.configure();

        assertThat(neo4j.getExposedPorts()).containsExactly(7687);
    }

    @Test
    void shouldRespectAlreadyDefinedPortMappingsHttp() {
        Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4").withExposedPorts(7474);

        neo4j.configure();

        assertThat(neo4j.getExposedPorts()).containsExactly(7474);
    }

    @Test
    void shouldRespectAlreadyDefinedPortMappingsWithoutHttps() {
        Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4").withExposedPorts(7687, 7474);

        neo4j.configure();

        assertThat(neo4j.getExposedPorts()).containsExactlyInAnyOrder(7474, 7687);
    }

    @Test
    void shouldDefaultExportBoltHttpAndHttps() {
        Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4");

        neo4j.configure();

        assertThat(neo4j.getExposedPorts()).containsExactlyInAnyOrder(7473, 7474, 7687);
    }

    @Test
    void shouldRespectCustomWaitStrategy() {
        Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4").waitingFor(new CustomDummyWaitStrategy());

        neo4j.configure();

        assertThat(neo4j).extracting("waitStrategy").isInstanceOf(CustomDummyWaitStrategy.class);
    }

    @Test
    void shouldConfigureSinglePluginByName() {
        try (Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4").withPlugins("apoc")) {
            // needs to get called explicitly for setup
            neo4j.configure();

            assertThat(neo4j.getEnvMap()).containsEntry("NEO4JLABS_PLUGINS", "[\"apoc\"]");
        }
    }

    @Test
    void shouldConfigureMultiplePluginsByName() {
        try (
            // configureLabsPlugins {
            Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4") //
                .withPlugins("apoc", "bloom");
            // }
        ) {
            // needs to get called explicitly for setup
            neo4j.configure();

            assertThat(neo4j.getEnvMap().get("NEO4JLABS_PLUGINS"))
                .containsAnyOf("[\"apoc\",\"bloom\"]", "[\"bloom\",\"apoc\"]");
        }
    }

    @Test
    void shouldCreateRandomUuidBasedPasswords() {
        try (
            // withRandomPassword {
            Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4").withRandomPassword();
            // }
        ) {
            // It will throw an exception if it's not UUID parsable.
            assertThatNoException().isThrownBy(neo4j::configure);
            // This basically is always true at if the random password is UUID-like.
            assertThat(neo4j.getAdminPassword())
                .satisfies(password -> assertThat(UUID.fromString(password).toString()).isEqualTo(password));
        }
    }

    @Test
    void shouldWarnOnPasswordTooShort() {
        try (Neo4jContainer neo4j = new Neo4jContainer("neo4j:4.4");) {
            Logger logger = (Logger) DockerLoggerFactory.getLogger("neo4j:4.4");
            TestLogAppender testLogAppender = new TestLogAppender();
            logger.addAppender(testLogAppender);
            testLogAppender.start();

            neo4j.withAdminPassword("short");

            testLogAppender.stop();

            assertThat(testLogAppender.passwordTooShortWarningAppeared).isTrue();
        }
    }

    private static class CustomDummyWaitStrategy extends AbstractWaitStrategy {

        @Override
        protected void waitUntilReady() {
            // ehm...ready
        }
    }

    private static class TestLogAppender extends AppenderBase<ILoggingEvent> {

        boolean passwordTooShortWarningAppeared = false;

        @Override
        protected void append(ILoggingEvent eventObject) {
            if (eventObject.getLevel().equals(Level.WARN)) {
                if (
                    eventObject
                        .getMessage()
                        .equals("Your provided admin password is too short and will not work with Neo4j 5.3+.")
                ) {
                    passwordTooShortWarningAppeared = true;
                }
            }
        }
    }

    private static Driver getDriver(Neo4jContainer neo4j) {
        AuthToken authToken = AuthTokens.none();
        if (neo4j.getAdminPassword() != null) {
            authToken = AuthTokens.basic("neo4j", neo4j.getAdminPassword());
        }
        return GraphDatabase.driver(neo4j.getBoltUrl(), authToken);
    }
}
