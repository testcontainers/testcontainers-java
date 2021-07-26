package org.testcontainers.images;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.DockerImageName;

/**
 * The default imagePullPolicy, which pulls the image from a remote repository only if it does not exist locally
 */
@Slf4j
@ToString
class DefaultPullPolicy extends AbstractImagePullPolicy {

    @Override
    protected boolean shouldPullCached(DockerImageName imageName, ImageData localImageData) {
        return false;
    }
}
