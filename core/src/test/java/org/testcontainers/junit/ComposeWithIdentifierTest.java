package org.testcontainers.junit;

import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.File;

public class ComposeWithIdentifierTest extends BaseComposeTest {

    @Container
    public ComposeContainer environment = new ComposeContainer(
        "TEST",
        new File("src/test/resources/v2-compose-test.yml")
    )
        .withExposedService("redis-1", REDIS_PORT);

    @Override
    protected ComposeContainer getEnvironment() {
        return this.environment;
    }
}
