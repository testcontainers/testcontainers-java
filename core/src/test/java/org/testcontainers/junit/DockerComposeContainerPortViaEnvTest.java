package org.testcontainers.junit;

import org.junit.jupiter.api.AutoClose;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.CommandLine;
import org.testcontainers.utility.DockerImageName;

import java.io.File;

import static org.assertj.core.api.Assumptions.assumeThat;

class DockerComposeContainerPortViaEnvTest extends BaseDockerComposeTest {

    @AutoClose
    public DockerComposeContainer environment = new DockerComposeContainer(
        DockerImageName.parse("docker:24.0.2"),
        new File("src/test/resources/v2-compose-test-port-via-env.yml")
    )
        .withExposedService("redis_1", REDIS_PORT)
        .withEnv("REDIS_PORT", String.valueOf(REDIS_PORT));

    DockerComposeContainerPortViaEnvTest() {
        assumeThat(CommandLine.runShellCommand("docker-compose", "--version"))
            .doesNotStartWith("Docker Compose version v2");
        environment.start();
    }

    @Override
    protected DockerComposeContainer getEnvironment() {
        return environment;
    }
}
