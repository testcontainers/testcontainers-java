package org.testcontainers.utility;

import org.junit.jupiter.api.Test;
import org.testcontainers.utility.ducttape.Timeouts;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link Timeouts} works correctly across shutdown/reuse cycles.
 * After {@code shutdown()} the executor is re-created on next use.
 */
class TimeoutsShutdownTest {

    @Test
    void timeoutsWorkAfterShutdown() {
        // First use
        String result1 = Timeouts.getWithTimeout(5, TimeUnit.SECONDS, () -> "container-1-ready");
        assertThat(result1).isEqualTo("container-1-ready");

        // Shutdown (as GenericContainer.stop() does)
        Timeouts.shutdown();

        // Second use — should transparently create a fresh executor
        String result2 = Timeouts.getWithTimeout(5, TimeUnit.SECONDS, () -> "container-2-ready");
        assertThat(result2).isEqualTo("container-2-ready");

        // Shutdown and use again to confirm repeatable
        Timeouts.shutdown();

        String result3 = Timeouts.getWithTimeout(5, TimeUnit.SECONDS, () -> "container-3-ready");
        assertThat(result3).isEqualTo("container-3-ready");
    }
}
