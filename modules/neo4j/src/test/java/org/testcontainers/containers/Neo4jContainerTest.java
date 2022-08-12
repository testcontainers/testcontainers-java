package org.testcontainers.containers;

import org.junit.Test;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.testcontainers.utility.MountableFile;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Tests of functionality special to the Neo4jContainer.
 *
 * @author Michael J. Simons
 */
public class Neo4jContainerTest {

    // See org.testcontainers.utility.LicenseAcceptance#ACCEPTANCE_FILE_NAME
    private static final String ACCEPTANCE_FILE_LOCATION = "/container-license-acceptance.txt";

    @Test
    public void shouldDisableAuthentication() {
        try (
            // spotless:off
            // withoutAuthentication {
            Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:4.4")
                .withoutAuthentication()
            // }
            // spotless:on
        ) {
            neo4jContainer.start();
            try (Driver driver = getDriver(neo4jContainer); Session session = driver.session()) {
                long one = session.run("RETURN 1", Collections.emptyMap()).next().get(0).asLong();
                assertThat(one).isEqualTo(1L);
            }
        }
    }

    @Test
    public void shouldCopyDatabase() {
        try (
            // copyDatabase {
            Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:3.5.30")
                .withDatabase(MountableFile.forClasspathResource("/test-graph.db"))
            // }
        ) {
            neo4jContainer.start();
            try (Driver driver = getDriver(neo4jContainer); Session session = driver.session()) {
                Result result = session.run("MATCH (t:Thing) RETURN t");
                assertThat(result.list().stream().map(r -> r.get("t").get("name").asString()))
                    .containsExactlyInAnyOrder("Thing", "Thing 2", "Thing 3", "A box");
            }
        }
    }

    @Test
    public void shouldFailOnCopyDatabaseForDefaultNeo4j4Image() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Neo4jContainer<>().withDatabase(MountableFile.forClasspathResource("/test-graph.db")))
            .withMessage("Copying database folder is not supported for Neo4j instances with version 4.0 or higher.");
    }

    @Test
    public void shouldFailOnCopyDatabaseForCustomNeo4j4Image() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> {
                new Neo4jContainer<>("neo4j:4.4.1").withDatabase(MountableFile.forClasspathResource("/test-graph.db"));
            })
            .withMessage("Copying database folder is not supported for Neo4j instances with version 4.0 or higher.");
    }

    @Test
    public void shouldFailOnCopyDatabaseForCustomNonSemverNeo4j4Image() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> {
                new Neo4jContainer<>("neo4j:latest").withDatabase(MountableFile.forClasspathResource("/test-graph.db"));
            })
            .withMessage("Copying database folder is not supported for Neo4j instances with version 4.0 or higher.");
    }

    @Test
    public void shouldCopyPlugins() {
        try (
            // registerPluginsPath {
            Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:4.4")
                .withPlugins(MountableFile.forClasspathResource("/custom-plugins"))
            // }
        ) {
            neo4jContainer.start();
            try (Driver driver = getDriver(neo4jContainer); Session session = driver.session()) {
                assertThatCustomPluginWasCopied(session);
            }
        }
    }

    @Test
    public void shouldCopyPlugin() {
        try (
            // registerPluginsJar {
            Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:4.4")
                .withPlugins(MountableFile.forClasspathResource("/custom-plugins/hello-world.jar"))
            // }
        ) {
            neo4jContainer.start();
            try (Driver driver = getDriver(neo4jContainer); Session session = driver.session()) {
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
    public void shouldCheckEnterpriseLicense() {
        assumeThat(Neo4jContainerTest.class.getResource(ACCEPTANCE_FILE_LOCATION)).isNull();

        String expectedImageName = "neo4j:4.4-enterprise";

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> new Neo4jContainer<>("neo4j:4.4").withEnterpriseEdition())
            .withMessageContaining("The image " + expectedImageName + " requires you to accept a license agreement.");
    }

    @Test
    public void shouldRunEnterprise() {
        assumeThat(Neo4jContainerTest.class.getResource(ACCEPTANCE_FILE_LOCATION)).isNotNull();

        try (
            // enterpriseEdition {
            Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:4.4")
                .withEnterpriseEdition()
                // }
                .withAdminPassword("Picard123")
        ) {
            neo4jContainer.start();
            try (Driver driver = getDriver(neo4jContainer); Session session = driver.session()) {
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
    public void shouldAddConfigToEnvironment() {
        // neo4jConfiguration {
        Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:4.4")
            .withNeo4jConfig("dbms.security.procedures.unrestricted", "apoc.*,algo.*")
            .withNeo4jConfig("dbms.tx_log.rotation.size", "42M");
        // }

        assertThat(neo4jContainer.getEnvMap())
            .containsEntry("NEO4J_dbms_security_procedures_unrestricted", "apoc.*,algo.*");
        assertThat(neo4jContainer.getEnvMap()).containsEntry("NEO4J_dbms_tx__log_rotation_size", "42M");
    }

    @Test
    public void shouldConfigureSingleLabsPlugin() {
        try (
            Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:4.4").withLabsPlugins(Neo4jLabsPlugin.APOC)
        ) {
            // needs to get called explicitly for setup
            neo4jContainer.configure();

            assertThat(neo4jContainer.getEnvMap()).containsEntry("NEO4JLABS_PLUGINS", "[\"apoc\"]");
        }
    }

    @Test
    public void shouldConfigureMultipleLabsPlugins() {
        try (
            // configureLabsPlugins {
            Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:4.4")
                .withLabsPlugins(Neo4jLabsPlugin.APOC, Neo4jLabsPlugin.BLOOM);
            // }
        ) {
            // needs to get called explicitly for setup
            neo4jContainer.configure();

            assertThat(neo4jContainer.getEnvMap().get("NEO4JLABS_PLUGINS"))
                .containsAnyOf("[\"apoc\",\"bloom\"]", "[\"bloom\",\"apoc\"]");
        }
    }

    @Test
    public void shouldConfigureSingleLabsPluginWithString() {
        try (Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:4.4").withLabsPlugins("myApoc")) {
            // needs to get called explicitly for setup
            neo4jContainer.configure();

            assertThat(neo4jContainer.getEnvMap()).containsEntry("NEO4JLABS_PLUGINS", "[\"myApoc\"]");
        }
    }

    @Test
    public void shouldConfigureMultipleLabsPluginsWithString() {
        try (
            Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:4.4").withLabsPlugins("myApoc", "myBloom")
        ) {
            // needs to get called explicitly for setup
            neo4jContainer.configure();

            assertThat(neo4jContainer.getEnvMap().get("NEO4JLABS_PLUGINS"))
                .containsAnyOf("[\"myApoc\",\"myBloom\"]", "[\"myBloom\",\"myApoc\"]");
        }
    }

    private static Driver getDriver(Neo4jContainer<?> container) {
        AuthToken authToken = AuthTokens.none();
        if (container.getAdminPassword() != null) {
            authToken = AuthTokens.basic("neo4j", container.getAdminPassword());
        }
        return GraphDatabase.driver(container.getBoltUrl(), authToken);
    }
}
