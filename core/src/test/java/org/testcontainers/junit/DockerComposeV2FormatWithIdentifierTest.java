package org.testcontainers.junit;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;

@Disabled
class DockerComposeV2FormatWithIdentifierTest extends BaseDockerComposeTest {

    @AutoClose
    public DockerComposeContainer environment = new DockerComposeContainer(
        DockerImageName.parse("docker/compose:1.29.2"),
        "TEST",
        new File("src/test/resources/v2-compose-test.yml")
    )
        .withExposedService("redis_1", REDIS_PORT);

    DockerComposeV2FormatWithIdentifierTest() {
        environment.start();
    }

    @Override
    protected DockerComposeContainer getEnvironment() {
        return this.environment;
    }
}
