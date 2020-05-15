package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNotNull;


public class DockerComposeContainerWithNameTest extends BaseDockerComposeTest {
    private final String REDIS_SERVICE_NAME = "redis";
    private final String REDIS_INSTANCE_NAME = "redis_1";
    private static final String DB_INSTANCE_NAME = "db_1";
    private static final int DB_PORT = 3306;

    @Rule
    public DockerComposeContainer environment =
        new DockerComposeContainer(new File("src/test/resources/compose-test-with-name.yml"))
            .withExposedService(REDIS_INSTANCE_NAME, REDIS_PORT)
            .withExposedService(DB_INSTANCE_NAME, DB_PORT);

    @Override
    public DockerComposeContainer getEnvironment() {
        return environment;
    }

    @Test
    public void testGetServicePort() {
        int serviceWithInstancePort = environment.getServicePort(REDIS_INSTANCE_NAME, REDIS_PORT);
        assertNotNull("Port is set for service with instance number", serviceWithInstancePort);
        int serviceWithoutInstancePort = environment.getServicePort(REDIS_SERVICE_NAME, REDIS_PORT);
        assertNotNull("Port is set for service with instance number", serviceWithoutInstancePort);
        assertEquals("Service ports are the same", serviceWithInstancePort, serviceWithoutInstancePort);
    }

    @Test
    public void containerNamesShouldBeCorrect() {
        String containerName = "/redis_container";
        Optional<ContainerState> result = environment.getContainerByServiceName(REDIS_INSTANCE_NAME);
        assertTrue(String.format("container should be found for service %s", REDIS_INSTANCE_NAME), result.isPresent());
        ContainerState state = result.get();
        assertEquals("container name should be same as compose file", containerName, state.getContainerInfo().getName());

        result = environment.getContainerByServiceName(DB_INSTANCE_NAME);
        assertTrue(String.format("container should be found for service %s", DB_INSTANCE_NAME), result.isPresent());
        state = result.get();
        assertTrue("container name should contain service name", state.getContainerInfo().getName().contains(DB_INSTANCE_NAME));
    }
}
