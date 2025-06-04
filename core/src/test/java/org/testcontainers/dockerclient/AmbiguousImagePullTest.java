package org.testcontainers.dockerclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.DockerRegistryContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

public class AmbiguousImagePullTest {

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void testNotUsingParse() {
        try (DockerRegistryContainer registryContainer = new DockerRegistryContainer()) {
            registryContainer.start();
            DockerImageName imageName = registryContainer.createImage("latest");
            String imageNameWithoutTag = imageName.getRegistry() + "/" + imageName.getRepository();
            try (
                final GenericContainer<?> container = new GenericContainer<>(imageNameWithoutTag).withExposedPorts(8080)
            ) {
                container.start();
                // do nothing other than start and stop
            }
        }
    }
}
