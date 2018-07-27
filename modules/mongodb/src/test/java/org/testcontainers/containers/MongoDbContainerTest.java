package org.testcontainers.containers;

import java.io.IOException;
import java.net.Socket;

import org.junit.Test;


public class MongoDbContainerTest {

    @Test
    public void containerStartsAndPublicPortIsAvailable() {
        try (MongoDbContainer container = new MongoDbContainer()) {
            container.start();
            assertThatPortIsAvailable(container.getPort());
        }
    }

    private void assertThatPortIsAvailable(Integer port) {
        try {
            new Socket("localhost", port);
        } catch (IOException e) {
            throw new AssertionError("The expected port " + port + " is not available!");
        }
    }

}
