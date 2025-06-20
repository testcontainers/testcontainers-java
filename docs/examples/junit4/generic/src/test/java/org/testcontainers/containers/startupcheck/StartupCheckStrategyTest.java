package org.testcontainers.containers.startupcheck;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Suite.class)
@Suite.SuiteClasses(
    {
        StartupCheckStrategyTest.OneShotStrategyTest.class,
        StartupCheckStrategyTest.IndefiniteOneShotStrategyTest.class,
        StartupCheckStrategyTest.MinimumDurationStrategyTest.class,
    }
)
@Testcontainers
public class StartupCheckStrategyTest {

    private static final String HELLO_TESTCONTAINERS = "Hello Testcontainers!";

    private static void waitForHello(GenericContainer container) throws TimeoutException {
        WaitingConsumer consumer = new WaitingConsumer();
        container.followOutput(consumer, OutputFrame.OutputType.STDOUT);

        consumer.waitUntil(frame -> frame.getUtf8String().contains(HELLO_TESTCONTAINERS), 30, TimeUnit.SECONDS);
    }

    @Nested
    public class OneShotStrategyTest {

        // spotless:off
        // withOneShotStrategy {
        @Container
        public GenericContainer<?> bboxWithOneShot = new GenericContainer<>(DockerImageName.parse("busybox:1.31.1"))
            .withCommand(String.format("echo %s", HELLO_TESTCONTAINERS))
            .withStartupCheckStrategy(
                new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(3))
            );

        // }
        // spotless:on

        @SneakyThrows
        @Test
        public void testCommandIsExecuted() {
            waitForHello(bboxWithOneShot);

            assertThat(bboxWithOneShot.isRunning()).isFalse();
        }
    }

    @Nested
    @Testcontainers
    public class IndefiniteOneShotStrategyTest {

        // spotless:off
        // withIndefiniteOneShotStrategy {
        @Container
        public GenericContainer<?> bboxWithIndefiniteOneShot = new GenericContainer<>(
            DockerImageName.parse("busybox:1.31.1")
        )
            .withCommand("sh", "-c", String.format("sleep 5 && echo \"%s\"", HELLO_TESTCONTAINERS))
            .withStartupCheckStrategy(
                new IndefiniteWaitOneShotStartupCheckStrategy()
            );

        // }
        // spotless:on

        @SneakyThrows
        @Test
        public void testCommandIsExecuted() {
            waitForHello(bboxWithIndefiniteOneShot);

            assertThat(bboxWithIndefiniteOneShot.isRunning()).isFalse();
        }
    }

    @Nested
    @Testcontainers
    public class MinimumDurationStrategyTest {

        // spotless:off
        // withMinimumDurationStrategy {
        @Container
        public GenericContainer<?> bboxWithMinimumDuration = new GenericContainer<>(
            DockerImageName.parse("busybox:1.31.1")
        )
            .withCommand("sh", "-c", String.format("sleep 5 && echo \"%s\"", HELLO_TESTCONTAINERS))
            .withStartupCheckStrategy(
                new MinimumDurationRunningStartupCheckStrategy(Duration.ofSeconds(1))
            );

        // }
        // spotless:on

        @SneakyThrows
        @Test
        public void testCommandIsExecuted() {
            assertThat(bboxWithMinimumDuration.isRunning()).isTrue();

            waitForHello(bboxWithMinimumDuration);
        }
    }
}
