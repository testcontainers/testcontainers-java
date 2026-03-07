package org.testcontainers.junit;

import org.junit.jupiter.api.AutoClose;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;

class ComposeWithIdentifierTest extends BaseComposeTest {

    @AutoClose
    public ComposeContainer environment = new ComposeContainer(
        DockerImageName.parse("docker:25.0.5"),
        "TEST",
        new File("src/test/resources/v2-compose-test.yml")
    )
        .withExposedService("redis-1", REDIS_PORT);

    ComposeWithIdentifierTest() {
        environment.start();
    }

    @Override
    protected ComposeContainer getEnvironment() {
        return this.environment;
    }
}
