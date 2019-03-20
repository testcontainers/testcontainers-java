package org.testcontainers.junit;

import java.io.File;
import org.junit.Rule;
import org.testcontainers.containers.DockerComposeContainer;

public class DockerComposeContainerWithBuildTest extends BaseDockerComposeTest {

    @Rule
    public DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withExposedService("redis_1", REDIS_PORT)
            .withExposedService("db_1", 3306)
            .withBuild(true)
            ;

    @Override
    protected DockerComposeContainer getEnvironment() {
        return environment;
    }
}
