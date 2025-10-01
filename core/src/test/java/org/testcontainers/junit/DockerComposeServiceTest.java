package org.testcontainers.junit;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DockerComposeServiceTest extends BaseDockerComposeTest {

    @AutoClose
    public DockerComposeContainer environment = new DockerComposeContainer(
        new File("src/test/resources/compose-test.yml")
    )
        .withServices("redis")
        .withExposedService("redis_1", REDIS_PORT);

    DockerComposeServiceTest() {
        environment.start();
    }

    @Override
    protected DockerComposeContainer getEnvironment() {
        return environment;
    }

    @Test
    void testDbIsNotStarting() {
        assertThatThrownBy(() -> {
                environment.getServicePort("db_1", 10001);
            })
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRedisIsStarting() {
        assertThat(environment.getServicePort("redis_1", REDIS_PORT)).as("Redis server started").isNotNull();
    }
}
