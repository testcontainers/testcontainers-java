package org.testcontainers.junit;

import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.File;

public class DockerComposeV2FormatWithIdentifierTest extends BaseDockerComposeTest {

    @Container
    public DockerComposeContainer environment = new DockerComposeContainer(
        "TEST",
        new File("src/test/resources/v2-compose-test.yml")
    )
        .withExposedService("redis_1", REDIS_PORT);

    @Override
    protected DockerComposeContainer getEnvironment() {
        return this.environment;
    }
}
