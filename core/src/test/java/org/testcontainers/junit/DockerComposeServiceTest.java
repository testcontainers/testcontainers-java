package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit4.TestcontainersRule;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class DockerComposeServiceTest extends BaseDockerComposeTest {

    @Rule
    public TestcontainersRule<DockerComposeContainer> environment = new TestcontainersRule<>(
        new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withServices("redis")
            .withExposedService("redis_1", REDIS_PORT)
    );

    @Override
    protected DockerComposeContainer getEnvironment() {
        return environment.get();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDbIsNotStarting() {
        environment.get().getServicePort("db_1", 10001);
    }

    @Test
    public void testRedisIsStarting() {
        assertThat(environment.get().getServicePort("redis_1", REDIS_PORT)).as("Redis server started").isNotNull();
    }
}
