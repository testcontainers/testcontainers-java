package org.testcontainers.junit.wait.strategy;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.testcontainers.TestImages;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit4.TestcontainersRule;

import java.time.Duration;

/**
 * Test wait strategy with overloaded waitingFor methods.
 * Other implementations of WaitStrategy are tested through backwards compatible wait strategy tests
 */
@RunWith(Enclosed.class)
public class HostPortWaitStrategyTest {

    public static class DefaultHostPortWaitStrategyTest {

        @ClassRule
        public static TestcontainersRule<GenericContainer<?>> container = new TestcontainersRule<>(
            new GenericContainer<>(TestImages.ALPINE_IMAGE)
                .withExposedPorts()
                .withCommand("sh", "-c", "while true; do nc -lp 8080; done")
                .withExposedPorts(8080)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10)))
        );

        @Test
        public void testWaiting() {}
    }

    public static class ExplicitHostPortWaitStrategyTest {

        @ClassRule
        public static TestcontainersRule<GenericContainer<?>> container = new TestcontainersRule<>(
            new GenericContainer<>(TestImages.ALPINE_IMAGE)
                .withExposedPorts()
                .withCommand("sh", "-c", "while true; do nc -lp 8080; done")
                .withExposedPorts(8080)
                .waitingFor(Wait.forListeningPorts(8080).withStartupTimeout(Duration.ofSeconds(10)))
        );

        @Test
        public void testWaiting() {}
    }
}
