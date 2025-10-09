package org.testcontainers.junit;

import org.junit.jupiter.api.AutoClose;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

class DockerComposeV2FormatWithIdentifierTest extends BaseDockerComposeTest {

    @AutoClose
    public DockerComposeContainer environment = new DockerComposeContainer(
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
