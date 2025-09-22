package org.testcontainers.junit;

import org.junit.jupiter.api.AutoClose;
import org.testcontainers.containers.ComposeContainer;

import java.io.File;

class ComposeWithIdentifierTest extends BaseComposeTest {

    @AutoClose
    public ComposeContainer environment = new ComposeContainer(
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
