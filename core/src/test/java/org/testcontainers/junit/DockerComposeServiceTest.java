package org.testcontainers.junit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class DockerComposeServiceTest extends BaseDockerComposeTest {

    public DockerComposeContainer environment = new DockerComposeContainer(
        new File("src/test/resources/compose-test.yml")
    )
        .withServices("redis")
        .withExposedService("redis_1", REDIS_PORT);

    @Before
    public void setUp() {
        environment.start();
    }

    @After
    public void cleanUp() {
        environment.stop();
    }

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
