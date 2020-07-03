package org.testcontainers;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

public interface TestImages {
    DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:3.0.2");
    DockerImageName RABBITMQ_IMAGE = DockerImageName.parse("rabbitmq:3.5.3");
    DockerImageName MONGODB_IMAGE = DockerImageName.parse("mongo:3.1.5");
    DockerImageName ALPINE_IMAGE = DockerImageName.parse("alpine:3.2");
    DockerImageName TINY_IMAGE = TestcontainersConfiguration.getInstance().getTinyDockerImageName();
}
