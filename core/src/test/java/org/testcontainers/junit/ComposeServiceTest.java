package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.ComposeContainer;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class ComposeServiceTest extends BaseComposeTest {

    @Rule
    public ComposeContainer environment = new ComposeContainer(new File("src/test/resources/compose-v2-test.yml"))
        .withServices("redis")
        .withExposedService("redis-1", REDIS_PORT);

    @Override
    protected ComposeContainer getEnvironment() {
        return this.environment;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDbIsNotStarting() {
        this.environment.getServicePort("db-1", 10001);
    }

    @Test
    public void testRedisIsStarting() {
        assertThat(this.environment.getServicePort("redis-1", REDIS_PORT)).as("Redis server started").isNotNull();
    }
}
