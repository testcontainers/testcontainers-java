package org.testcontainers;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * TODO: Javadocs
 */
public interface TestingImages {
    public static final DockerImageName REDIS_IMAGE = new DockerImageName("redis:3.0.2");
    public static final DockerImageName RABBITMQ_IMAGE = new DockerImageName("rabbitmq:3.5.3");
    public static final DockerImageName MONGODB_IMAGE = new DockerImageName("mongo:3.1.5");
    public static final DockerImageName ALPINE_IMAGE = new DockerImageName("alpine:3.2");
    public static final DockerImageName TINY_IMAGE = TestcontainersConfiguration.getInstance().getTinyDockerImageName();
}
