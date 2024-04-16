package org.testcontainers.junit;

import org.junit.Rule;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

public class DockerComposeContainerPortViaEnvTest extends BaseDockerComposeTest {

    @Rule
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
