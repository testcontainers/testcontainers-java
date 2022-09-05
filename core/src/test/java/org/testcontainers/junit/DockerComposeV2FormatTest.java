package org.testcontainers.junit;

import org.junit.After;
import org.junit.Before;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

/**
 * Created by rnorth on 21/05/2016.
 */
public class DockerComposeV2FormatTest extends BaseDockerComposeTest {

    public DockerComposeContainer environment = new DockerComposeContainer(
        new File("src/test/resources/v2-compose-test.yml")
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
