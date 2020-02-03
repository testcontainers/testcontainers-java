package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNotNull;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

/**
 * Created by rnorth on 08/08/2015.
 */
public class DockerComposeContainerTest extends BaseDockerComposeTest {

    @Rule
    public DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
        .withExposedService("redis_1", REDIS_PORT)
        .withExposedService("db_1", 3306);

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

    @Test
    public void shouldRetrieveContainerByServiceName() {
        String existingServiceName = "db_1";
        Optional<ContainerState> result = environment.getContainerByServiceName(existingServiceName);
        assertTrue(format("Container should be found by service name %s", existingServiceName), result.isPresent());
        assertEquals("Mapped port for result container was wrong, probably wrong container found", result.get().getExposedPorts(), singletonList(3306));
    }

    @Test
    public void shouldReturnEmptyResultOnNoneExistingService() {
        String notExistingServiceName = "db_256";
        Optional<ContainerState> result = environment.getContainerByServiceName(notExistingServiceName);
        assertFalse(format("No container should be found under service name %s", notExistingServiceName), result.isPresent());
    }
}
