package org.testcontainers.containers;

import java.io.IOException;
import java.net.Socket;

import org.junit.Test;


public class MongoDbContainerTest {

    @Test
    public void containerStartsAndPublicPortIsAvailable() {
        try (MongoDbContainer container = new MongoDbContainer()) {
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
