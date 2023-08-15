package org.testcontainers.junit;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DockerComposeServiceTest extends BaseDockerComposeTest {

    @Container
    public DockerComposeContainer environment = new DockerComposeContainer(
        new File("src/test/resources/compose-test.yml")
    )
        .withServices("redis")
        .withExposedService("redis_1", REDIS_PORT);

    @Override
    protected DockerComposeContainer getEnvironment() {
        return environment;
    }

    @Test
    public void testDbIsNotStarting() {
        assertThatThrownBy(() -> environment.getServicePort("db_1", 10001))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testRedisIsStarting() {
        assertThat(environment.getServicePort("redis_1", REDIS_PORT)).as("Redis server started").isNotNull();
    }
}
