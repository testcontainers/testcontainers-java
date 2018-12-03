package org.testcontainers.containers;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;

import java.util.Collections;

import org.junit.Test;
import org.neo4j.driver.v1.AuthToken;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;

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
            Neo4jContainer neo4jContainer = new Neo4jContainer().withAdminPassword(null);
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
    public void shouldCheckEnterpriseLicense() {
        assumeThat(Neo4jContainerTest.class.getResource(ACCEPTANCE_FILE_LOCATION)).isNull();

        String expectedImageName = "neo4j:3.5.0-enterprise";

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> new Neo4jContainer().withEnterpriseEdition())
            .withMessageContaining("The image " + expectedImageName + " requires you to accept a license agreement.");
    }

    @Test
    public void shouldRunEnterprise() {
        assumeThat(Neo4jContainerTest.class.getResource(ACCEPTANCE_FILE_LOCATION)).isNotNull();

        try (
            Neo4jContainer neo4jContainer = new Neo4jContainer()
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

    private static Driver getDriver(Neo4jContainer container) {

        AuthToken authToken = AuthTokens.none();
        if (container.getAdminPassword() != null) {
            authToken = AuthTokens.basic("neo4j", container.getAdminPassword());
        }
        return GraphDatabase.driver(container.getBoltUrl(), authToken);
    }
}
