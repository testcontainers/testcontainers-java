package org.testcontainers.junit;

import org.junit.Rule;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit4.TestcontainersRule;

import java.io.File;

public class DockerComposeV2WithNetworkTest extends BaseDockerComposeTest {

    @Rule
    public TestcontainersRule<DockerComposeContainer> environment = new TestcontainersRule<>(
        new DockerComposeContainer(new File("src/test/resources/v2-compose-test-with-network.yml"))
            .withExposedService("redis_1", REDIS_PORT)
    );

    @Override
    protected DockerComposeContainer getEnvironment() {
        return environment.get();
    }
}
