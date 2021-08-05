package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface TestImages {
    DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:3.0.2");
    DockerImageName RABBITMQ_IMAGE = DockerImageName.parse("rabbitmq:3.5.3");
    DockerImageName MONGODB_IMAGE = DockerImageName.parse("mongo:3.1.5");
    DockerImageName ALPINE_IMAGE = DockerImageName.parse("alpine:3.14");
    DockerImageName DOCKER_REGISTRY_IMAGE = DockerImageName.parse("registry:2.7.0");
    DockerImageName TINY_IMAGE = DockerImageName.parse("alpine:3.14");
}
