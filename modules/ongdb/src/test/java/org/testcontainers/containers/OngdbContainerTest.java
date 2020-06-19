package org.testcontainers.containers;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;

import java.util.Collections;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.driver.v1.AuthToken;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.testcontainers.utility.MountableFile;

/**
 * Tests of functionality special to the OngdbContainer.
 *
 * @author bradnussbaum
 */
public class OngdbContainerTest {

    @Test
    public void shouldDisableAuthentication() {

        try (
            OngdbContainer ongdbContainer = new OngdbContainer().withoutAuthentication();
        ) {
            ongdbContainer.start();
            try (Driver driver = getDriver(ongdbContainer);
                Session session = driver.session()
            ) {
                long one = session.run("RETURN 1", Collections.emptyMap()).next().get(0).asLong();
                assertThat(one).isEqualTo(1L);
            }
        }
    }

    @Test
    @Ignore( "Fails due to permission error from copy")
    public void shouldCopyDatabase() {
        try (
            OngdbContainer ongdbContainer = new OngdbContainer()
                .withDatabase(MountableFile.forClasspathResource("/test-graph.db"));
        ) {
            ongdbContainer.start();
            try (
                Driver driver = getDriver(ongdbContainer);
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
            OngdbContainer ongdbContainer = new OngdbContainer()
                .withPlugins(MountableFile.forClasspathResource("/custom-plugins"));
        ) {
            ongdbContainer.start();
            try (
                Driver driver = getDriver(ongdbContainer);
                Session session = driver.session()
            ) {
                assertThatCustomPluginWasCopied(session);
            }
        }
    }

    @Test
    public void shouldCopyPlugin() {
        try (
            OngdbContainer ongdbContainer = new OngdbContainer()
                .withPlugins(MountableFile.forClasspathResource("/custom-plugins/hello-world.jar"));
        ) {
            ongdbContainer.start();
            try (
                Driver driver = getDriver(ongdbContainer);
                Session session = driver.session()
            ) {
                assertThatCustomPluginWasCopied(session);
            }
        }
    }

    private static void assertThatCustomPluginWasCopied(Session session) {
        StatementResult result = session.run("RETURN gf.tc.helloWorld('Testcontainers') AS greeting");
        Record singleRecord = result.single();
        assertThat(singleRecord).isNotNull();
        assertThat(singleRecord.get("greeting").asString()).isEqualTo("Hello, Testcontainers");
    }

    @Test
    public void shouldAddConfigToEnvironment() {

        OngdbContainer ongdbContainer = new OngdbContainer()
            .withConfig("dbms.security.procedures.unrestricted", "apoc.*,algo.*")
            .withConfig("dbms.tx_log.rotation.size", "42M");

        assertThat(ongdbContainer.getEnvMap())
            .containsEntry("NEO4J_dbms_security_procedures_unrestricted", "apoc.*,algo.*");
        assertThat(ongdbContainer.getEnvMap())
            .containsEntry("NEO4J_dbms_tx__log_rotation_size", "42M");
    }

    private static Driver getDriver(OngdbContainer container) {

        AuthToken authToken = AuthTokens.none();
        if (container.getAdminPassword() != null) {
            authToken = AuthTokens.basic("neo4j", container.getAdminPassword());
        }
        return GraphDatabase.driver(container.getBoltUrl(), authToken);
    }
}
