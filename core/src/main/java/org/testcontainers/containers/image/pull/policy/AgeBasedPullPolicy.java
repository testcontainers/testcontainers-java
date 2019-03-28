package org.testcontainers.containers.image.pull.policy;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.image.ImageData;

/**
 * An ImagePullPolicy which pulls the image from a remote repository only if its created date is older than maxAge
 */
@Slf4j
public class AgeBasedPullPolicy implements ImagePullPolicy {

    private long maxAge;
    private TimeUnit unit;

    /**
     * @param maxAge - Maximum age of image (based on image Created parameter)
     * @param unit - The TimeUnit to use (MINUTES, HOURS, DAYS, etc.)
     */
    public AgeBasedPullPolicy(long maxAge, TimeUnit unit) {
        this.maxAge = maxAge;
        this.unit = unit;
    }

    @Override
    public boolean shouldPull(ImageData image) {
        boolean result = unit.convert(Instant.now().getEpochSecond() - image.getCreated(), TimeUnit.SECONDS) > maxAge;
        if (result) {
            log.trace("Should pull image with tags: {}", Arrays.asList(image.getRepoTags()));
        }
        return result;
    }
}
