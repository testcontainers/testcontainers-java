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
 * @author bradnussbaum
 */
public class OngdbContainerIntegrationTest
{

    @ClassRule
    public static OngdbContainer ongdbContainer = new OngdbContainer();

    @Test
    public void shouldStart() {

        boolean actual = ongdbContainer.isRunning();
        assertThat(actual).isTrue();

        try (Driver driver = GraphDatabase
            .driver(ongdbContainer.getBoltUrl(), AuthTokens.basic("neo4j", "password"));
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
        String actual = ongdbContainer.getBoltUrl();

        assertThat(actual).isNotNull();
        assertThat(actual).startsWith("bolt://");
    }

    @Test
    public void shouldReturnHttpUrl() {
        String actual = ongdbContainer.getHttpUrl();

        assertThat(actual).isNotNull();
        assertThat(actual).startsWith("http://");
    }
}
