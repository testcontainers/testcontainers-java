package com.example;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
    classes = DemoApplication.class,
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = { "spring.datasource.url=jdbc:tc:postgresql:11-alpine:///databasename" }
)
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:3-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        redis.start();
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }
}
