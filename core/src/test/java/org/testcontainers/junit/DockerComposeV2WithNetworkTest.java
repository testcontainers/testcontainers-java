package org.testcontainers.junit;

import java.io.File;

import org.junit.Rule;
import org.testcontainers.containers.DockerComposeContainer;

public class DockerComposeV2WithNetworkTest extends BaseDockerComposeTest {

    @Rule
    public DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/v2-compose-test-with-network.yml"))
            .withExposedService("redis_1", REDIS_PORT);

    @Override
    protected DockerComposeContainer getEnvironment() {
        return environment;
    }



}
