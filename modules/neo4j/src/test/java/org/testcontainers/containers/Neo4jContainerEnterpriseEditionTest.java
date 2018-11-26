package org.testcontainers.containers;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;

public class Neo4jContainerEnterpriseEditionTest {

    @ClassRule
    public static Neo4jContainer neo4jContainer = new Neo4jContainer()
        .withEnterpriseEdition()
        .acceptLicense()
        .withAdminPassword("Picard123");

    @Test
    public void shouldRunEnterprise() {

        try (Driver driver = GraphDatabase.driver(neo4jContainer.getBoltUrl(), AuthTokens.basic("neo4j", "Picard123"));
            Session session = driver.session()
        ) {
            String edition = session.run("CALL dbms.components() YIELD edition RETURN edition", Collections.emptyMap())
                .next().get(0).asString();
            assertThat(edition, is("enterprise"));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
