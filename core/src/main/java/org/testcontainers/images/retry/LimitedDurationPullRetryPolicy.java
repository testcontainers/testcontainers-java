package org.testcontainers.images.retry;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;

/**
 * An ImagePullRetryPolicy which will retry a failed image pull if the time elapsed since the
 * pull started is less than or equal to the configured {@link #maxAllowedDuration}.
 *
 */
@Slf4j
@ToString
public class LimitedDurationPullRetryPolicy implements ImagePullRetryPolicy {

    @Getter
    private final Duration maxAllowedDuration;

    Instant lastRetryAllowed;

    public LimitedDurationPullRetryPolicy(Duration maxAllowedDuration) {
        if (maxAllowedDuration == null) {
            throw new NullPointerException("maxAllowedDuration should not be null");
        }

        if (maxAllowedDuration.isNegative()) {
            throw new IllegalArgumentException("maxAllowedDuration should not be negative");
        }

        this.maxAllowedDuration = maxAllowedDuration;
    }

    @Override
    public void pullStarted() {
        this.lastRetryAllowed = Instant.now().plus(maxAllowedDuration);
    }

    @Override
    public boolean shouldRetry(DockerImageName imageName, Throwable error) {
        if (lastRetryAllowed == null) {
            throw new IllegalStateException("lastRetryAllowed is null. Please, check that pullStarted has been called.");
        }

        if (Instant.now().isBefore(lastRetryAllowed)) {
            log.warn(
                "Retrying pull for image: {} ({}s remaining)",
                imageName,
                Duration.between(Instant.now(), lastRetryAllowed).getSeconds()
            );

            return true;
        }

        return false;
    }
}
