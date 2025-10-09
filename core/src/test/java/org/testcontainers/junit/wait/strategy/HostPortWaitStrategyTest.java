package org.testcontainers.junit.wait.strategy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.TestImages;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

/**
 * Test wait strategy with overloaded waitingFor methods.
 * Other implementations of WaitStrategy are tested through backwards compatible wait strategy tests
 */
class HostPortWaitStrategyTest {

    @Nested
    class DefaultHostPortWaitStrategyTest {

        public GenericContainer<?> container = new GenericContainer<>(TestImages.ALPINE_IMAGE)
            .withExposedPorts()
            .withCommand("sh", "-c", "while true; do nc -lp 8080; done")
            .withExposedPorts(8080)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10)));

        @Test
        void testWaiting() {
            container.start();
        }
    }

    @Nested
    class ExplicitHostPortWaitStrategyTest {

        public GenericContainer<?> container = new GenericContainer<>(TestImages.ALPINE_IMAGE)
            .withExposedPorts()
            .withCommand("sh", "-c", "while true; do nc -lp 8080; done")
            .withExposedPorts(8080)
            .waitingFor(Wait.forListeningPorts(8080).withStartupTimeout(Duration.ofSeconds(10)));

        @Test
        void testWaiting() {
            container.start();
        }
    }
}
