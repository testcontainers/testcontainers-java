package org.testcontainers.dockerclient;

import org.junit.Test;
import org.testcontainers.DockerRegistryContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class AmbiguousImagePullTest {

    @Test(timeout = 30_000)
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
