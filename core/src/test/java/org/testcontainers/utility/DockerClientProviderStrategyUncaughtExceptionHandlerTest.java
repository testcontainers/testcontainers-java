package org.testcontainers.utility;

import org.junit.jupiter.api.Test;
import org.awaitility.Awaitility;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;

class DockerClientProviderStrategyUncaughtExceptionHandlerTest {

    @Test
    void dockerClientProviderStrategyTestShouldUseDontCatchUncaughtExceptions() throws Exception {
        byte[] bytes;
        try (InputStream is = getClass()
            .getClassLoader()
            .getResourceAsStream("org/testcontainers/dockerclient/DockerClientProviderStrategy.class")) {
            assertThat(is).isNotNull();
            bytes = is.readAllBytes();
        }

        // Use ISO_8859_1 to preserve a stable 1:1 mapping of bytes to chars for a lightweight constant-pool substring check.
        String content = new String(bytes, StandardCharsets.ISO_8859_1);
        assertThat(content).contains("dontCatchUncaughtExceptions");
    }

    @Test
    void shouldNotInterceptUncaughtExceptionsFromOtherThreads() throws Exception {
        Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        AtomicReference<Throwable> observedException = new AtomicReference<>();
        CountDownLatch observedExceptionLatch = new CountDownLatch(1);
        Thread.UncaughtExceptionHandler expectedHandler = (t, e) -> {
            if (observedException.compareAndSet(null, e)) {
                observedExceptionLatch.countDown();
            }
        };

        Thread.setDefaultUncaughtExceptionHandler(expectedHandler);
        try {
            AtomicReference<Thread.UncaughtExceptionHandler> changedHandler = new AtomicReference<>();
            AtomicBoolean monitorStop = new AtomicBoolean(false);
            Thread monitor = new Thread(() -> {
                while (!monitorStop.get()) {
                    Thread.UncaughtExceptionHandler current = Thread.getDefaultUncaughtExceptionHandler();
                    if (current != expectedHandler) {
                        changedHandler.compareAndSet(null, current);
                        break;
                    }
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
                }
            }, "docker-strategy-uncaught-handler-monitor");

            Thread thrower = new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                    return;
                }
                throw new RuntimeException("boom");
            }, "docker-strategy-uncaught-handler-thrower");

            monitor.start();
            thrower.start();

            Awaitility
                .await()
                .dontCatchUncaughtExceptions()
                .pollDelay(Duration.ZERO)
                .pollInterval(Duration.ofMillis(25))
                .atMost(Duration.ofSeconds(2))
                .until(() -> {
                    Thread.sleep(300);
                    return true;
                });

            monitorStop.set(true);
            monitor.join(TimeUnit.SECONDS.toMillis(5));
            thrower.join(TimeUnit.SECONDS.toMillis(5));

            assertThat(changedHandler.get()).isNull();
            assertThat(observedExceptionLatch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(observedException.get()).isInstanceOf(RuntimeException.class).hasMessage("boom");
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler);
        }
    }
}
