package org.testcontainers.core;

import org.testcontainers.images.ImagePullPolicy;
import org.testcontainers.images.PullPolicy;

import java.time.Duration;

public interface Configuration {
    int getPriority();

    /**
     * in seconds
     * @return
     */
    default Duration getRyukTimeout() {
        return Duration.ofSeconds(30);
    }

    default boolean environmentSupportsReuse() {
        return false;
    }

    default String getDockerClientStrategyClassName() {
        return null;
    }

    default String getTransportType() {
        return "httpclient5";
    }

    /**
     * in seconds
     * @return
     */
    default Duration getImagePullPauseTimeout() {
        return Duration.ofSeconds(30);
    }

    default String getImageSubstitutorClassName() {
        return null;
    }

    /**
     * in seconds
     * @return
     */
    default Duration getClientPingTimeout() {
        return Duration.ofSeconds(10);
    }

    default ImagePullPolicy getImagePullPolicy() {
        return PullPolicy.defaultPolicy();
    }
}
