package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNotNull;

/**
 * Created by rnorth on 08/08/2015.
 */
public class DockerComposeContainerTest extends BaseDockerComposeTest {

    @Rule
    public DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withExposedService("redis_1", REDIS_PORT)
            .withExposedService("db_1", 3306)
            ;

    @Override
    protected DockerComposeContainer getEnvironment() {
        return environment;
    }

    @Test
    public void testGetServicePort() {
        int serviceWithInstancePort = environment.getServicePort("redis_1", REDIS_PORT);
        assertNotNull("Port is set for service with instance number", serviceWithInstancePort);
        int serviceWithoutInstancePort = environment.getServicePort("redis", REDIS_PORT);
        assertNotNull("Port is set for service with instance number", serviceWithoutInstancePort);
        assertEquals("Service ports are the same", serviceWithInstancePort, serviceWithoutInstancePort);
    }
}
