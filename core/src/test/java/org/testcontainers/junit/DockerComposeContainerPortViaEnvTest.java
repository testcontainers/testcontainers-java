package org.testcontainers.junit;

import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;

@Testcontainers
public class DockerComposeContainerPortViaEnvTest extends BaseDockerComposeTest {

    @Container
    public DockerComposeContainer environment = new DockerComposeContainer(
        new File("src/test/resources/v2-compose-test-port-via-env.yml")
    )
        .withExposedService("redis_1", REDIS_PORT)
        .withEnv("REDIS_PORT", String.valueOf(REDIS_PORT));

    @Override
    protected DockerComposeContainer getEnvironment() {
        return environment;
    }
}
