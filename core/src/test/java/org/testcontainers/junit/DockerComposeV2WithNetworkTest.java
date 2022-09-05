package org.testcontainers.junit;

import org.junit.After;
import org.junit.Before;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

public class DockerComposeV2WithNetworkTest extends BaseDockerComposeTest {

    public DockerComposeContainer environment = new DockerComposeContainer(
        new File("src/test/resources/v2-compose-test-with-network.yml")
    )
        .withExposedService("redis_1", REDIS_PORT);

    @Before
    public void setUp() {
        environment.start();
    }

    @After
    public void cleanUp() {
        environment.stop();
    }

    @Override
    protected DockerComposeContainer getEnvironment() {
        return environment;
    }
}
