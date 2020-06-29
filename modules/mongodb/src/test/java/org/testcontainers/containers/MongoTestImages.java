package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public interface MongoTestImages {
    DockerImageName MONGO_IMAGE_NAME = new DockerImageName("mongo:4.0.10");
}
