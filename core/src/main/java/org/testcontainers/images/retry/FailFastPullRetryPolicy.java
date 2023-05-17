package org.testcontainers.images.retry;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.DockerImageName;

/**
 * Fail-fast, i.e. not retry, pull policy
 */
@Slf4j
@ToString
public class FailFastPullRetryPolicy implements ImagePullRetryPolicy {

    @Override
    public boolean shouldRetry(DockerImageName imageName, Throwable error) {
        return false;
    }
}
