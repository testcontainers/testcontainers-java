package org.testcontainers.junit.wait.strategy;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.TestImages;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

import static org.rnorth.visibleassertions.VisibleAssertions.pass;

/**
 * Test wait strategy with overloaded waitingFor methods.
 * Other implementations of WaitStrategy are tested through backwards compatible wait strategy tests
 */
public class HostPortWaitStrategyTest {

    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>(TestImages.ALPINE_IMAGE).withExposedPorts()
        .withCommand("sh", "-c", "while true; do nc -lp 8080; done")
        .withExposedPorts(8080)
        .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10)));

    @Test
    public void testWaiting() {
        pass("Container starts after waiting");
    }
}
