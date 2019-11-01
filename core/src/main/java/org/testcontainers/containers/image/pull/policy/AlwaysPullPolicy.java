package org.testcontainers.containers.image.pull.policy;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.image.ImageData;

/***
 * An ImagePullPolicy which pulls the image even if it exists locally.
 * Useful for obtaining the latest version of an image with a static tag, i.e. 'latest'
 */
@Slf4j
public class AlwaysPullPolicy implements ImagePullPolicy {

    @Override
    public boolean shouldPull(ImageData image) {
        log.trace("Unconditionally pulling an image with tags: {}", Arrays.asList(image.getRepoTags()));
        return true;
    }
}
