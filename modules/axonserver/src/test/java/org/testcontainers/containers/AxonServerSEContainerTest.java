package org.testcontainers.containers;

import org.junit.*;
import org.testcontainers.utility.DockerImageName;


public class AxonServerSEContainerTest {

    @Test
    public void supportsAxonServer_4_4_X() {
        try (
            final AxonServerSEContainer axonServerSEContainer =
                new AxonServerSEContainer(DockerImageName.parse("axoniq/axonserver:4.4.12"))
        ) {
            axonServerSEContainer.start();
        }
    }

    @Test
    public void supportsAxonServer_4_5_X() {
        try (
            final AxonServerSEContainer axonServerSEContainer =
                new AxonServerSEContainer(DockerImageName.parse("axoniq/axonserver:4.5.8"))
        ) {
            axonServerSEContainer.start();
        }
    }
}
