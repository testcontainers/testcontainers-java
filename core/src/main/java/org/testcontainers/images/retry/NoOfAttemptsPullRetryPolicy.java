package org.testcontainers.images.retry;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.DockerImageName;

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

    private int currentNoOfAttempts = 0;

    public NoOfAttemptsPullRetryPolicy(Integer maxAllowedNoOfAttempts) {
        if (maxAllowedNoOfAttempts == null) {
            throw new NullPointerException("maxAllowedNoOfAttempts should not be null");
        }

        if (maxAllowedNoOfAttempts < 0) {
            throw new IllegalArgumentException("maxAllowedNoOfAttempts should not be negative");
        }

        this.maxAllowedNoOfAttempts = maxAllowedNoOfAttempts;
    }

    @Override
    public boolean shouldRetry(DockerImageName imageName, Throwable error) {
        return ++currentNoOfAttempts <= maxAllowedNoOfAttempts;
    }
}
