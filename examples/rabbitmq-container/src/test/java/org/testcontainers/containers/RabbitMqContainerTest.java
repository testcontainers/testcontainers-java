package org.testcontainers.containers;

import org.junit.Test;

import java.io.IOException;
import java.net.Socket;


public class RabbitMqContainerTest {

    @Test
    public void containerStartsAndPublicPortIsAvailable() {
        try (RabbitMqContainer container = new RabbitMqContainer()) {
            container.start();
            assertThatPortIsAvailable(container);
        }
    }

    @Test
    public void waitStrategyWorksForVersion37() {
        try (RabbitMqContainer container = new RabbitMqContainer("rabbitmq:3.7")) {
            container.start();
            assertThatPortIsAvailable(container);
        }
    }

    @Test
    public void waitStrategyWorksVersion36() {
        try (RabbitMqContainer container = new RabbitMqContainer("rabbitmq:3.6")) {
            container.start();
            assertThatPortIsAvailable(container);
        }
    }

    private void assertThatPortIsAvailable(RabbitMqContainer container) {
        try {
            new Socket(container.getContainerIpAddress(), container.getPort());
        } catch (IOException e) {
            throw new AssertionError("The expected port " + container.getPort() + " is not available!");
        }
    }

}
