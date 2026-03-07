package org.testcontainers.junit;

import org.junit.jupiter.api.AutoClose;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;

class ComposeContainerPortViaEnvTest extends BaseComposeTest {

    @AutoClose
    public ComposeContainer environment = new ComposeContainer(
        DockerImageName.parse("docker:25.0.5"),
        new File("src/test/resources/v2-compose-test-port-via-env.yml")
    )
        .withExposedService("redis-1", REDIS_PORT)
        .withEnv("REDIS_PORT", String.valueOf(REDIS_PORT));

    ComposeContainerPortViaEnvTest() {
        this.environment.start();
    }

    @Override
    protected ComposeContainer getEnvironment() {
        return environment;
    }
}
