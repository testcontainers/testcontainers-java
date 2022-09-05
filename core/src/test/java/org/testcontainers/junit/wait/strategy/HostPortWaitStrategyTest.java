package org.testcontainers.junit.wait.strategy;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.TestImages;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

/**
 * Test wait strategy with overloaded waitingFor methods.
 * Other implementations of WaitStrategy are tested through backwards compatible wait strategy tests
 */
public class HostPortWaitStrategyTest {

    public static GenericContainer<?> container = new GenericContainer<>(TestImages.ALPINE_IMAGE)
        .withExposedPorts()
        .withCommand("sh", "-c", "while true; do nc -lp 8080; done")
        .withExposedPorts(8080)
        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10)));

    @BeforeClass
    public static void setUp() {
        container.start();
    }

    @AfterClass
    public static void cleanUp() {
        container.stop();
    }

    @Test
    public void testWaiting() {}
}
