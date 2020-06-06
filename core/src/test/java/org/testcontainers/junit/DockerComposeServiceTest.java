package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;

import static org.rnorth.visibleassertions.VisibleAssertions.assertNotNull;

import java.io.File;

public class DockerComposeServiceTest extends BaseDockerComposeTest {

    @Rule
    public DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withServices("redis")
            .withExposedService("redis_1", REDIS_PORT);


    @Override
    protected DockerComposeContainer getEnvironment() {
        return environment;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDbIsNotStarting() {
        environment.getServicePort("db_1", 10001);
    }

    @Test
    public void testRedisIsStarting() {
        assertNotNull("Redis server started", environment.getServicePort("redis_1", REDIS_PORT));
    }
}
