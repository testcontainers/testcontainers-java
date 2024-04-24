package org.testcontainers.utility;

import org.testcontainers.images.AbstractImagePullPolicy;
import org.testcontainers.images.ImageData;

public class FakeImagePullPolicy extends AbstractImagePullPolicy {

    @Override
    protected boolean shouldPullCached(DockerImageName imageName, ImageData localImageData) {
        return false;
    }
}
