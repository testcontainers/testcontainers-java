package com.example;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractIntegrationTest {

    public static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:3.0.6"))
            .withExposedPorts(6379);

    static {
        redis.start();
    }
}
