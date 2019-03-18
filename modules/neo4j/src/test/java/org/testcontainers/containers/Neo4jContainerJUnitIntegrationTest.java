package org.testcontainers.containers;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;

import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;

/**
 * Test for basic functionality when used as a <code>@ClassRule</code>.
 *
 * @author Michael J. Simons
 */
public class Neo4jContainerJUnitIntegrationTest {

    @ClassRule
    public static Neo4jContainer neo4jContainer = new Neo4jContainer();

    @Test
    public void shouldStart() {

        boolean actual = neo4jContainer.isRunning();
        assertThat(actual).isTrue();

        try (Driver driver = GraphDatabase
            .driver(neo4jContainer.getBoltUrl(), AuthTokens.basic("neo4j", "password"));
            Session session = driver.session()
        ) {
            long one = session.run("RETURN 1", Collections.emptyMap()).next().get(0).asLong();
            assertThat(one).isEqualTo(1L);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void shouldReturnBoltUrl() {
        String actual = neo4jContainer.getBoltUrl();

        assertThat(actual).isNotNull();
        assertThat(actual).startsWith("bolt://");
    }

    @Test
    public void shouldReturnHttpUrl() {
        String actual = neo4jContainer.getHttpUrl();

        assertThat(actual).isNotNull();
        assertThat(actual).startsWith("http://");
    }
}
