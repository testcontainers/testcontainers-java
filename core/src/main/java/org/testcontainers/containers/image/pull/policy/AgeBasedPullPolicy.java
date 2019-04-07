package org.testcontainers.containers.image.pull.policy;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.image.ImageData;

/**
 * An ImagePullPolicy which pulls the image from a remote repository only if its created date is older than maxAge
 */
@Slf4j
public class AgeBasedPullPolicy implements ImagePullPolicy {

    private Duration maxAge;

    /**
     * @param maxAge - Maximum allowed age of image (based on image Created parameter)
     */
    public AgeBasedPullPolicy(Duration maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public boolean shouldPull(ImageData image) {
        Duration imageAge = Duration.between(Instant.ofEpochSecond(image.getCreated()), Instant.now());
        boolean result = imageAge.compareTo(maxAge) > 0;
        if (result) {
            log.trace("Should pull image with tags: {}", Arrays.asList(image.getRepoTags()));
        }
        return result;
    }
}
