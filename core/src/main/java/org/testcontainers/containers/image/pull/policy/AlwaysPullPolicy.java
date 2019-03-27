package org.testcontainers.containers.image.pull.policy;

import com.github.dockerjava.api.model.Image;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AlwaysPullPolicy implements ImagePullPolicy {

    @Override
    public boolean shouldPull(Image image) {
        log.trace("Should pull image with tags: {}", Arrays.asList(image.getRepoTags()));
        return true;
    }
}
