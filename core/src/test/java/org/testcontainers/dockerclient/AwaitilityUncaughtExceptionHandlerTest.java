package org.testcontainers.dockerclient;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.lang.Thread.UncaughtExceptionHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for <a href="https://github.com/testcontainers/testcontainers-java/issues/11483">#11483</a>.
 *
 * <p>{@code DockerClientProviderStrategy.test()} pings the Docker socket through an executor-backed
 * {@code Awaitility.await()...untilAsserted(...)}. With Awaitility's default (uncaught-exception catching enabled),
 * Awaitility installs its own {@link UncaughtExceptionHandler} as the JVM-global default handler for the duration of
 * that await, hijacking the application's handler (see the stack trace in the issue). The probed condition runs on a
 * single dedicated thread and never relies on exceptions surfacing from other threads, so the catching provides no
 * value here and the strategy opts out via {@code dontCatchUncaughtExceptions()}.
 */
class AwaitilityUncaughtExceptionHandlerTest {

    private static final UncaughtExceptionHandler SENTINEL = (thread, throwable) -> {};

    @Test
    void defaultExecutorPollingHijacksTheGlobalHandler() {
        // Documents the broken behaviour: while the await runs, the global handler is no longer the application's one.
        assertThat(handlerSeenWhilePolling(Awaitility.await())).isNotSameAs(SENTINEL);
    }

    @Test
    void dontCatchUncaughtExceptionsKeepsTheGlobalHandler() {
        // The fix applied in DockerClientProviderStrategy#test(): the application's handler stays in place throughout.
        assertThat(handlerSeenWhilePolling(Awaitility.await().dontCatchUncaughtExceptions())).isSameAs(SENTINEL);
    }

    /**
     * Installs {@link #SENTINEL} as the global default handler, then drives an executor-backed await exactly as
     * {@code DockerClientProviderStrategy#test()} does, and returns the handler that was active <em>while</em> polling.
     */
    private static UncaughtExceptionHandler handlerSeenWhilePolling(org.awaitility.core.ConditionFactory factory) {
        UncaughtExceptionHandler original = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(SENTINEL);
        AtomicReference<UncaughtExceptionHandler> observed = new AtomicReference<>();
        try {
            factory
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(10))
                .pollDelay(Duration.ZERO)
                .untilAsserted(() -> observed.set(Thread.getDefaultUncaughtExceptionHandler()));
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(original);
        }
        return observed.get();
    }
}
