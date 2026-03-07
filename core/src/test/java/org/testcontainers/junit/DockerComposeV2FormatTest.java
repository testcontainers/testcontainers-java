package org.testcontainers.junit;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;

/**
 * Created by rnorth on 21/05/2016.
 */
@Disabled
class DockerComposeV2FormatTest extends BaseDockerComposeTest {

    @AutoClose
    public DockerComposeContainer environment = new DockerComposeContainer(
        DockerImageName.parse("docker/compose:1.29.2"),
        new File("src/test/resources/v2-compose-test.yml")
    )
        .withExposedService("redis_1", REDIS_PORT);

    DockerComposeV2FormatTest() {
        environment.start();
    }

    @Override
    protected DockerComposeContainer getEnvironment() {
        return environment;
    }
}
