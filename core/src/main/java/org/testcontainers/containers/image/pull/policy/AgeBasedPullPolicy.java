package org.testcontainers.containers.image.pull.policy;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.image.ImageData;

@Slf4j
public class AgeBasedPullPolicy implements ImagePullPolicy {

    private long maxAge;
    private TimeUnit unit;

    public AgeBasedPullPolicy(long maxAge, TimeUnit unit) {
        this.maxAge = maxAge;
        this.unit = unit;
    }

    @Override
    public boolean shouldPull(ImageData image) {
        boolean result = unit.convert(System.currentTimeMillis() - 1000 * (long) image.getCreated(), TimeUnit.MILLISECONDS) > maxAge;
        if (result) {
            log.trace("Should pull image with tags: {}", Arrays.asList(image.getRepoTags()));
        }
        return result;
    }
}
