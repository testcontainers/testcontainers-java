package org.testcontainers.junit;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.CommandLine;
import org.testcontainers.utility.DockerImageName;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

class DockerComposeServiceTest extends BaseDockerComposeTest {

    @AutoClose
    public DockerComposeContainer environment = new DockerComposeContainer(
        DockerImageName.parse("docker:24.0.2"),
        new File("src/test/resources/compose-test.yml")
    )
        .withServices("redis")
        .withExposedService("redis_1", REDIS_PORT);

    DockerComposeServiceTest() {
        assumeThat(CommandLine.runShellCommand("docker-compose", "--version"))
            .doesNotStartWith("Docker Compose version v2");
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
