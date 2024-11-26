package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface TestImages {
    DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:6-alpine");
    DockerImageName RABBITMQ_IMAGE = DockerImageName.parse("rabbitmq:3.7.25");
    DockerImageName MONGODB_IMAGE = DockerImageName.parse("mongo:4.4");
    DockerImageName ALPINE_IMAGE = DockerImageName.parse("alpine:3.17");
    DockerImageName DOCKER_REGISTRY_IMAGE = DockerImageName.parse("registry:2.7.0");
    DockerImageName TINY_IMAGE = DockerImageName.parse("alpine:3.17");
}
