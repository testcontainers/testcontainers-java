package org.testcontainers.containers.image.pull.policy;

import org.testcontainers.containers.image.ImageData;

public interface ImagePullPolicy {

    boolean shouldPull(ImageData image);

}
