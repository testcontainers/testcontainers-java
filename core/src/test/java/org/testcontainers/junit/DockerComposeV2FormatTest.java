package org.testcontainers.junit;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.TestEnvironment;

import java.io.File;

/**
 * Created by rnorth on 21/05/2016.
 */
public class DockerComposeV2FormatTest extends BaseDockerComposeTest {

    @Before
    public void checkVersion() {
        Assume.assumeTrue(TestEnvironment.dockerApiAtLeast("1.22"));
    }

    @Rule
    public DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/v2-compose-test.yml"))
            .withExposedService("redis_1", REDIS_PORT);

    @Override
    protected DockerComposeContainer getEnvironment() {
        return environment;
    }
}
