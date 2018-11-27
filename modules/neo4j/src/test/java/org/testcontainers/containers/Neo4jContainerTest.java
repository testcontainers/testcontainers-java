package org.testcontainers.containers;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

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
                assertThat(one, is(1L));
            }
        }
    }

    @Test
    public void shouldRunEnterprise() {

        try (
            Neo4jContainer neo4jContainer = new Neo4jContainer()
                .withEnterpriseEdition()
                .acceptLicense()
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
                assertThat(edition, is("enterprise"));
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
