package org.testcontainers;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

public interface TestingImages {
    DockerImageName REDIS_IMAGE = new DockerImageName("redis:3.0.2");
    DockerImageName RABBITMQ_IMAGE = new DockerImageName("rabbitmq:3.5.3");
    DockerImageName MONGODB_IMAGE = new DockerImageName("mongo:3.1.5");
    DockerImageName ALPINE_IMAGE = new DockerImageName("alpine:3.2");
    DockerImageName TINY_IMAGE = TestcontainersConfiguration.getInstance().getTinyDockerImageName();
}
