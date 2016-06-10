package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;
import redis.clients.jedis.Jedis;

import java.io.File;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

/**
 * Created by rnorth on 08/08/2015.
 */
public class DockerComposeContainerTest {

    private static final int REDIS_PORT = 6379;

    @Rule
    public DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withExposedService("redis_1", REDIS_PORT)
            .withExposedService("db_1", 3306)
            ;

    @Test
    public void simpleTest() {

        Jedis jedis = new Jedis(environment.getServiceHost("redis_1", REDIS_PORT), environment.getServicePort("redis_1", REDIS_PORT));

        jedis.incr("test");
        jedis.incr("test");
        jedis.incr("test");

        assertEquals("A redis instance defined in compose can be used in isolation", 3, Integer.valueOf(jedis.get("test")));
    }

    @Test
    public void secondTest() {
        // used in manual checking for cleanup in between tests
        Jedis jedis = new Jedis(environment.getServiceHost("redis_1", REDIS_PORT), environment.getServicePort("redis_1", REDIS_PORT));

        jedis.incr("test");
        jedis.incr("test");
        jedis.incr("test");

        assertEquals("Tests use fresh container instances", 3, Integer.valueOf(jedis.get("test")));
        // if these end up using the same container one of the test methods will fail.
        // However, @Rule creates a separate DockerComposeContainer instance per test, so this just shouldn't happen
    }
}
