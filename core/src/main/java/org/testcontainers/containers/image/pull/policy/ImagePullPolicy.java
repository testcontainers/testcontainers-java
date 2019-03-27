package org.testcontainers.containers.image.pull.policy;

import com.github.dockerjava.api.model.Image;

@FunctionalInterface
public interface ImagePullPolicy {

    boolean shouldPull(Image image);

}
