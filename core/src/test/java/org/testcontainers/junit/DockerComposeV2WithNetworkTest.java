package org.testcontainers.junit;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;

@Disabled
class DockerComposeV2WithNetworkTest extends BaseDockerComposeTest {

    @AutoClose
    public DockerComposeContainer environment = new DockerComposeContainer(
        DockerImageName.parse("docker:24.0.2"),
        new File("src/test/resources/v2-compose-test-with-network.yml")
    )
        .withExposedService("redis_1", REDIS_PORT);

    DockerComposeV2WithNetworkTest() {
        environment.start();
    }

    @Override
    protected DockerComposeContainer getEnvironment() {
        return environment;
    }
}
