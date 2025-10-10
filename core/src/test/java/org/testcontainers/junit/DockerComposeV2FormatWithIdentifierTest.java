package org.testcontainers.junit;

import org.junit.jupiter.api.AutoClose;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.CommandLine;
import org.testcontainers.utility.DockerImageName;

import java.io.File;

import static org.assertj.core.api.Assumptions.assumeThat;

class DockerComposeV2FormatWithIdentifierTest extends BaseDockerComposeTest {

    @AutoClose
    public DockerComposeContainer environment = new DockerComposeContainer(
        DockerImageName.parse("docker:24.0.2"),
        "TEST",
        new File("src/test/resources/v2-compose-test.yml")
    )
        .withExposedService("redis_1", REDIS_PORT);

    DockerComposeV2FormatWithIdentifierTest() {
        assumeThat(CommandLine.runShellCommand("docker-compose", "--version"))
            .doesNotStartWith("Docker Compose version v2");
        environment.start();
    }

    @Override
    protected DockerComposeContainer getEnvironment() {
        return this.environment;
    }
}
