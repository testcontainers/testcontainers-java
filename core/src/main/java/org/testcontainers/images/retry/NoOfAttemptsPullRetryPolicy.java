package org.testcontainers.images.retry;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.DockerImageName;

/**
 * An ImagePullRetryPolicy which will retry a failed image pull if the number of attempts
 * is less than or equal to the configured {@link #maxAllowedNoOfAttempts}.
 *
 */
@Slf4j
@RequiredArgsConstructor
@ToString
public class NoOfAttemptsPullRetryPolicy implements ImagePullRetryPolicy {

    @NonNull
    @Getter
    private final int maxAllowedNoOfAttempts;

    private int currentNoOfAttempts = 0;

    @Override
    public boolean shouldRetry(DockerImageName imageName, Throwable error) {
        return ++currentNoOfAttempts > maxAllowedNoOfAttempts;
    }
}
