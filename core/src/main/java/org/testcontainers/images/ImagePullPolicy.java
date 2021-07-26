package org.testcontainers.images;

import org.testcontainers.utility.DockerImageName;

public interface ImagePullPolicy {

    boolean shouldPull(DockerImageName imageName);

}
