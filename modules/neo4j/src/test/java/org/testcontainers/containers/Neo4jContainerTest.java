package org.testcontainers.containers;

import org.junit.Test;
import org.neo4j.driver.v1.AuthToken;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.testcontainers.utility.MountableFile;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
            Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>(Neo4jTestImages.NEO4J_TEST_IMAGE).withoutAuthentication()
        ) {
            neo4jContainer.start();
            try (Driver driver = getDriver(neo4jContainer);
                Session session = driver.session()
            ) {
                long one = session.run("RETURN 1", Collections.emptyMap()).next().get(0).asLong();
                assertThat(one).isEqualTo(1L);
            }
        }
    }

    @Test
    public void shouldCopyDatabase() {
        try (
            Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>(Neo4jTestImages.NEO4J_TEST_IMAGE)
                .withDatabase(MountableFile.forClasspathResource("/test-graph.db"))
        ) {
            neo4jContainer.start();
            try (
                Driver driver = getDriver(neo4jContainer);
                Session session = driver.session()
            ) {
                StatementResult result = session.run("MATCH (t:Thing) RETURN t");
                assertThat(result.list().stream().map(r -> r.get("t").get("name").asString()))
                    .containsExactlyInAnyOrder("Thing", "Thing 2", "Thing 3", "A box");
            }
        }
    }

    @Test
    public void shouldCopyPlugins() {
        try (
            Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>(Neo4jTestImages.NEO4J_TEST_IMAGE)
                .withPlugins(MountableFile.forClasspathResource("/custom-plugins"))
        ) {
            neo4jContainer.start();
            try (
                Driver driver = getDriver(neo4jContainer);
                Session session = driver.session()
            ) {
                assertThatCustomPluginWasCopied(session);
            }
        }
    }

    @Test
    public void shouldCopyPlugin() {
        try (
            Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>(Neo4jTestImages.NEO4J_TEST_IMAGE)
                .withPlugins(MountableFile.forClasspathResource("/custom-plugins/hello-world.jar"))
        ) {
            neo4jContainer.start();
            try (
                Driver driver = getDriver(neo4jContainer);
                Session session = driver.session()
            ) {
                assertThatCustomPluginWasCopied(session);
            }
        }
    }

    private static void assertThatCustomPluginWasCopied(Session session) {
        StatementResult result = session.run("RETURN ac.simons.helloWorld('Testcontainers') AS greeting");
        Record singleRecord = result.single();
        assertThat(singleRecord).isNotNull();
        assertThat(singleRecord.get("greeting").asString()).isEqualTo("Hello, Testcontainers");
    }

    @Test
    public void shouldCheckEnterpriseLicense() {
        assumeThat(Neo4jContainerTest.class.getResource(ACCEPTANCE_FILE_LOCATION)).isNull();

        String expectedImageName = "neo4j:3.5.0-enterprise";

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> new Neo4jContainer<>(Neo4jTestImages.NEO4J_TEST_IMAGE).withEnterpriseEdition())
            .withMessageContaining("The image " + expectedImageName + " requires you to accept a license agreement.");
    }

    @Test
    public void shouldRunEnterprise() {
        assumeThat(Neo4jContainerTest.class.getResource(ACCEPTANCE_FILE_LOCATION)).isNotNull();

        try (
            Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>(Neo4jTestImages.NEO4J_TEST_IMAGE)
                .withEnterpriseEdition()
                .withAdminPassword("Picard123")
        ) {
            neo4jContainer.start();
            try (
                Driver driver = getDriver(neo4jContainer);
                Session session = driver.session()
            ) {
                String edition = session
                    .run("CALL dbms.components() YIELD edition RETURN edition", Collections.emptyMap())
                    .next().get(0).asString();
                assertThat(edition).isEqualTo("enterprise");
            }
        }
    }

    @Test
    public void shouldAddConfigToEnvironment() {

        Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>(Neo4jTestImages.NEO4J_TEST_IMAGE)
            .withNeo4jConfig("dbms.security.procedures.unrestricted", "apoc.*,algo.*")
            .withNeo4jConfig("dbms.tx_log.rotation.size", "42M");

        assertThat(neo4jContainer.getEnvMap())
            .containsEntry("NEO4J_dbms_security_procedures_unrestricted", "apoc.*,algo.*");
        assertThat(neo4jContainer.getEnvMap())
            .containsEntry("NEO4J_dbms_tx__log_rotation_size", "42M");
    }

    private static Driver getDriver(Neo4jContainer<?> container) {

        AuthToken authToken = AuthTokens.none();
        if (container.getAdminPassword() != null) {
            authToken = AuthTokens.basic("neo4j", container.getAdminPassword());
        }
        return GraphDatabase.driver(container.getBoltUrl(), authToken);
    }
}
