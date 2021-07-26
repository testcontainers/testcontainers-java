package org.testcontainers.images;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.DockerImageName;

/***
 * An ImagePullPolicy which pulls the image even if it exists locally.
 * Useful for obtaining the latest version of an image with a static tag, i.e. 'latest'
 */
@Slf4j
@ToString
class AlwaysPullPolicy implements ImagePullPolicy {

    @Override
    public boolean shouldPull(DockerImageName imageName) {
        log.trace("Unconditionally pulling an image: {}", imageName);
        return true;
    }
}
