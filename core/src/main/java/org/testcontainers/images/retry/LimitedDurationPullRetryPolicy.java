package org.testcontainers.images.retry;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@ToString
public class LimitedDurationPullRetryPolicy implements ImagePullRetryPolicy {

    @NonNull
    @Getter
    Duration maxAllowedDuration;

    Instant lastRetryAllowed;

    @Override
    public void pullStarted() {
        this.lastRetryAllowed = Instant.now().plus(maxAllowedDuration);
    }

    @Override
    public boolean shouldRetry(DockerImageName imageName, Throwable error) {
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
