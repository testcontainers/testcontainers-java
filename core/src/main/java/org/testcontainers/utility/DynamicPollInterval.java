package org.testcontainers.utility;

import org.awaitility.pollinterval.PollInterval;

import java.time.Duration;
import java.time.Instant;

/**
 * Awaitility {@link org.awaitility.pollinterval.PollInterval} that takes execution time into consideration,
 * to allow a constant poll-interval, as opposed to Awaitility's default poll-delay behaviour.
 *
 * @deprecated For internal usage only.
 */
@Deprecated
public class DynamicPollInterval implements PollInterval {

    final Duration interval;
    Instant lastTimestamp;

    public DynamicPollInterval(Duration interval) {
        this.interval = interval;
        lastTimestamp = Instant.now();
    }

    @Override
    public Duration next(int pollCount, Duration previousDuration) {
        Instant now = Instant.now();
        Duration executionDuration = Duration.between(lastTimestamp, now);

        Duration result = interval.minusMillis(Math.min(interval.toMillis(), executionDuration.toMillis()));
        lastTimestamp = now.plus(result);
        return result;
    }
}
