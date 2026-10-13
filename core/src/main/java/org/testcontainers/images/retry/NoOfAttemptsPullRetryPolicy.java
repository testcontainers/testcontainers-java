package org.testcontainers.images.retry;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * An ImagePullRetryPolicy which will retry a failed image pull if the number of attempts
 * is less than or equal to the configured {@link #maxAllowedNoOfAttempts}.
 *
 */
@Slf4j
@ToString
public class NoOfAttemptsPullRetryPolicy implements ImagePullRetryPolicy {

    @Getter
    private final int maxAllowedNoOfAttempts;

    private final AtomicInteger currentNoOfAttempts = new AtomicInteger(0);

    public NoOfAttemptsPullRetryPolicy(int maxAllowedNoOfAttempts) {
        if (maxAllowedNoOfAttempts < 0) {
            throw new IllegalArgumentException("maxAllowedNoOfAttempts should not be negative");
        }

        this.maxAllowedNoOfAttempts = maxAllowedNoOfAttempts;
    }

    @Override
    public void pullStarted() {
        currentNoOfAttempts.set(0);
    }

    @Override
    public boolean shouldRetry(DockerImageName imageName, Throwable error) {
        return currentNoOfAttempts.incrementAndGet() <= maxAllowedNoOfAttempts;
    }
}
