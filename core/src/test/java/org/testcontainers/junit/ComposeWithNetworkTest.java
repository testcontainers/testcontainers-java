package org.testcontainers.junit;

import org.junit.jupiter.api.AutoClose;
import org.testcontainers.containers.ComposeContainer;

import java.io.File;

class ComposeWithNetworkTest extends BaseComposeTest {

    @AutoClose
    public ComposeContainer environment = new ComposeContainer(
        new File("src/test/resources/v2-compose-test-with-network.yml")
    )
        .withExposedService("redis-1", REDIS_PORT);

    ComposeWithNetworkTest() {
        environment.start();
    }

    @Override
    protected ComposeContainer getEnvironment() {
        return environment;
    }
}
