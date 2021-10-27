package org.testcontainers.containers;

import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.Socket;


public class MongoDbContainerTest {

    @Test
    public void containerStartsAndPublicPortIsAvailable() {
        try (MongoDbContainer container = new MongoDbContainer(DockerImageName.parse("mongo:4.0"))) {
            container.start();
            assertThatPortIsAvailable(container);
        }
    }

    private void assertThatPortIsAvailable(MongoDbContainer container) {
        try {
            new Socket(container.getContainerIpAddress(), container.getPort());
        } catch (IOException e) {
            throw new AssertionError("The expected port " + container.getPort() + " is not available!");
        }
    }

}
