package org.testcontainers.junit;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.TestEnvironment;
import redis.clients.jedis.Jedis;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class DockerComposeV2Test {

    protected static final int REDIS_PORT = 6379;

    @BeforeClass
    public static void checkVersion() {
        Assume.assumeTrue(TestEnvironment.dockerApiAtLeast("1.22"));
    }

    @Test
    public void simpleTest() {
        try (
            DockerComposeContainer environment = new DockerComposeContainer(
                new File("src/test/resources/v2-compose-test.yml")
            )
                .withExposedService("redis-1", REDIS_PORT)
        ) {
            environment.start();
            Jedis jedis = new Jedis(
                environment.getServiceHost("redis-1", REDIS_PORT),
                environment.getServicePort("redis-1", REDIS_PORT)
            );

            jedis.incr("test");
            jedis.incr("test");
            jedis.incr("test");

            assertThat(jedis.get("test"))
                .as("A redis instance defined in compose can be used in isolation")
                .isEqualTo("3");
        }
    }
}
