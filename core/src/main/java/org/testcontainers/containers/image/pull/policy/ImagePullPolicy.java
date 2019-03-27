package org.testcontainers.containers.image.pull.policy;

import org.testcontainers.containers.image.ImageData;

@FunctionalInterface
public interface ImagePullPolicy {

    boolean shouldPull(ImageData image);

}
