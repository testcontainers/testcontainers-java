package org.testcontainers.junit;

import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.File;

public class ComposeContainerPortViaEnvTest extends BaseComposeTest {

    @Container
    public ComposeContainer environment = new ComposeContainer(
        new File("src/test/resources/v2-compose-test-port-via-env.yml")
    )
        .withExposedService("redis-1", REDIS_PORT)
        .withEnv("REDIS_PORT", String.valueOf(REDIS_PORT));

    @Override
    protected ComposeContainer getEnvironment() {
        return environment;
    }
}
