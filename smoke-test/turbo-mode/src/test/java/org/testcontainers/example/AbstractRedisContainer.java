package org.testcontainers.example;

import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractRedisContainer {

    private static final String REDIS_IMAGE = "redis:7.0.12-alpine";

    void runRedisContainer() {
        try (
            GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE)
                .withExposedPorts(6379)
                .withCreateContainerCmdModifier(cmd -> cmd.withName("tc-redis"))
        ) {
            redis.start();
            assertThat(redis.isRunning()).isTrue();
        }
    }
}
