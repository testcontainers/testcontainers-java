package org.testcontainers.example;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractRedisContainer {

    private static final String REDIS_IMAGE = "redis:7.0.12-alpine";

    void imageIsNotAvailableBeforeToRun() {
        DockerClient dockerClient = DockerClientFactory.lazyClient();
        List<Image> images = dockerClient.listImagesCmd().withReferenceFilter(REDIS_IMAGE).exec();
        assertThat(images).hasSize(0);
        try (GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379)) {
            redis.start();
            assertThat(redis.isRunning()).isTrue();
        }
    }
}
