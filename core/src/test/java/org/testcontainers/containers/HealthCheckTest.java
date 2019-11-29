package org.testcontainers.containers;

import com.github.dockerjava.api.model.HealthCheck;
import org.junit.Test;

import java.util.Arrays;

import static org.rnorth.visibleassertions.VisibleAssertions.*;

public class HealthCheckTest {

    @Test
    public void containerStartWithHealthCheck(){

        final HealthCheck healthCheck = new HealthCheck()
            .withRetries(2)
            .withTest(Arrays.asList("ls"));

        try(GenericContainer container = new GenericContainer<>()
            .withHealthCheck(healthCheck)) {
            container.start();
            assertTrue("Container is running", container.getContainerInfo().getState().getRunning());
        }
    }
}
