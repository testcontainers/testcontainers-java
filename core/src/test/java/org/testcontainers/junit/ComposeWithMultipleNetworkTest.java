package org.testcontainers.junit;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class ComposeWithMultipleNetworkTest extends BaseComposeTest {

    @AutoClose
    public ComposeContainer environment = new ComposeContainer(
        DockerImageName.parse("docker:25.0.5"),
        new File("src/test/resources/v2-compose-test-with-multiple-networks.yml")
    )
        .withExposedService("redis-1", REDIS_PORT)
        .withExposedService("another-redis-1", REDIS_PORT);

    ComposeWithMultipleNetworkTest() {
        environment.start();
    }

    @Override
    protected ComposeContainer getEnvironment() {
        return environment;
    }

    @Test
    void redisInstanceInDifferentNetworkCanBeUsed() {
        Jedis jedis = new Jedis(
            getEnvironment().getServiceHost("another-redis-1", REDIS_PORT),
            getEnvironment().getServicePort("another-redis-1", REDIS_PORT)
        );

        jedis.incr("test");
        jedis.incr("test");
        jedis.incr("test");

        assertThat(jedis.get("test")).as("A redis instance defined in compose can be used in isolation").isEqualTo("3");
    }
}
