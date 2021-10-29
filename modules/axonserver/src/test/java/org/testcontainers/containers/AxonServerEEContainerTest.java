package org.testcontainers.containers;

import org.junit.*;
import org.testcontainers.utility.DockerImageName;


public class AxonServerEEContainerTest {

    @Test
    public void supportsAxonServer_4_5_X() {
        try (
            final AxonServerEEContainer axonServerEEContainer =
                new AxonServerEEContainer(DockerImageName.parse("axoniq/axonserver-enterprise:4.5.9-dev"))
        ) {
            axonServerEEContainer.start();
        }
    }
}
