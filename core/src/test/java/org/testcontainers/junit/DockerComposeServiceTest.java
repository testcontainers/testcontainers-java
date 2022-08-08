package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class DockerComposeServiceTest extends BaseDockerComposeTest {

    @Rule
    public DockerComposeContainer environment = new DockerComposeContainer(
        new File("src/test/resources/compose-test.yml")
    )
        .withServices("redis")
        .withExposedService("redis_1", REDIS_PORT);

    @Override
    protected DockerComposeContainer getEnvironment() {
        return environment;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDbIsNotStarting() {
        environment.getServicePort("db_1", 10001);
    }

    @Test
    public void testRedisIsStarting() {
        assertThat(environment.getServicePort("redis_1", REDIS_PORT)).as("Redis server started").isNotNull();
    }
}
