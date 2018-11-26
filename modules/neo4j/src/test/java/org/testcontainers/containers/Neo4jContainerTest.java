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

public class Neo4jContainerTest {

    @ClassRule
    public static Neo4jContainer neo4jContainer = new Neo4jContainer();

    @Test
    public void shouldStart() {
        boolean actual = neo4jContainer.isRunning();

        assertThat(actual, is(true));

        try (Driver driver = GraphDatabase
            .driver(neo4jContainer.getBoltUrl(), AuthTokens.basic("neo4j", "password"));
            Session session = driver.session()
        ) {
            long one = session.run("RETURN 1", Collections.emptyMap()).next().get(0).asLong();
            assertThat(one, is(1L));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void shouldReturnBoltUrl() {
        String actual = neo4jContainer.getBoltUrl();

        assertThat(actual, notNullValue());
        assertThat(actual, startsWith("bolt://"));
    }

    @Test
    public void shouldReturnHttpUrl() {
        String actual = neo4jContainer.getHttpUrl();

        assertThat(actual, notNullValue());
        assertThat(actual, startsWith("http://"));
    }
}
