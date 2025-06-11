package org.testcontainers.junit.wait.strategy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.TestImages;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

/**
 * Test wait strategy with overloaded waitingFor methods.
 * Other implementations of WaitStrategy are tested through backwards compatible wait strategy tests
 */
@Testcontainers
public class HostPortWaitStrategyTest {

    @Nested
    public class DefaultHostPortWaitStrategyTest {

        @Container
        public GenericContainer<?> container = new GenericContainer<>(TestImages.ALPINE_IMAGE)
            .withExposedPorts()
            .withCommand("sh", "-c", "while true; do nc -lp 8080; done")
            .withExposedPorts(8080)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10)));

        @Test
        public void testWaiting() {}
    }

    @Nested
    public class ExplicitHostPortWaitStrategyTest {

        @Container
        public GenericContainer<?> container = new GenericContainer<>(TestImages.ALPINE_IMAGE)
            .withExposedPorts()
            .withCommand("sh", "-c", "while true; do nc -lp 8080; done")
            .withExposedPorts(8080)
            .waitingFor(Wait.forListeningPorts(8080).withStartupTimeout(Duration.ofSeconds(10)));

        @Test
        public void testWaiting() {}
    }
}
