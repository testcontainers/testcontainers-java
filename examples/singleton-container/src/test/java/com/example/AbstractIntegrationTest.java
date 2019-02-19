package com.example;

import org.testcontainers.containers.GenericContainer;

public abstract class AbstractIntegrationTest {

    public static final GenericContainer redis = new GenericContainer("redis:3.0.6")
            .withExposedPorts(6379);

    static {
        redis.start();
    }
}
