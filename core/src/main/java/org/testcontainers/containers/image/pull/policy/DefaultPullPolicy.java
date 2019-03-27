package org.testcontainers.containers.image.pull.policy;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.image.ImageData;

@Slf4j
public class DefaultPullPolicy implements ImagePullPolicy {

    @Override
    public boolean shouldPull(ImageData image) {
        log.trace("Should pull image with tags: {}", Arrays.asList(image.getRepoTags()));
        return false;
    }
}
