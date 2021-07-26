package org.testcontainers.images;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;

/**
 * An ImagePullPolicy which pulls the image from a remote repository only if its created date is older than maxAge
 */
@Slf4j
@Value
class AgeBasedPullPolicy extends AbstractImagePullPolicy {

    Duration maxAge;

    @Override
    protected boolean shouldPullCached(DockerImageName imageName, ImageData localImageData) {
        Duration imageAge = Duration.between(localImageData.getCreatedAt(), Instant.now());
        boolean result = imageAge.compareTo(maxAge) > 0;
        if (result) {
            log.trace("Should pull image: {}", imageName);
        }
        return result;
    }
}
