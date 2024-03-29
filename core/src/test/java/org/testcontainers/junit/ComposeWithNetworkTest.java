package org.testcontainers.junit;

import org.junit.Rule;
import org.testcontainers.containers.ComposeContainer;

import java.io.File;

public class ComposeWithNetworkTest extends BaseComposeTest {

    @Rule
    public ComposeContainer environment = new ComposeContainer(
        new File("src/test/resources/v2-compose-test-with-network.yml")
    )
        .withExposedService("redis-1", REDIS_PORT);

    @Override
    protected ComposeContainer getEnvironment() {
        return environment;
    }
}
