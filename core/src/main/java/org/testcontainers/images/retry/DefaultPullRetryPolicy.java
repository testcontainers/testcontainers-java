package org.testcontainers.images.retry;

import com.github.dockerjava.api.exception.InternalServerErrorException;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.Duration;

/**
 * Default pull retry policy.
 *
 * Will retry on <code>InterruptedException</code> and <code>InternalServerErrorException</code>
 * exceptions for a time limit of two minutes.
 */
@Slf4j
@ToString
public class DefaultPullRetryPolicy extends LimitedDurationPullRetryPolicy {

    private static final Duration PULL_RETRY_TIME_LIMIT = Duration.ofSeconds(
        TestcontainersConfiguration.getInstance().getImagePullTimeout()
    );

    public DefaultPullRetryPolicy() {
        super(PULL_RETRY_TIME_LIMIT);
    }

    @Override
    public boolean shouldRetry(DockerImageName imageName, Throwable error) {
        if (!mayRetry(error)) {
            return false;
        }

        return super.shouldRetry(imageName, error);
    }

    private boolean mayRetry(Throwable error) {
        return error instanceof InterruptedException || error instanceof InternalServerErrorException;
    }
}
