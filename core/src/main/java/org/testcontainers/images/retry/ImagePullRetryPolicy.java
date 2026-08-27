package org.testcontainers.images.retry;

import org.testcontainers.utility.DockerImageName;

public interface ImagePullRetryPolicy {
    default void pullStarted() {}

    boolean shouldRetry(DockerImageName imageName, Throwable error);
}
