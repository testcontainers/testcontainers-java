package com.example.compose;

import com.example.DemoApplication;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;

@RunWith(SpringJUnit4ClassRunner.class)
@Testcontainers
@SpringBootTest(
    classes = DemoApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = { "spring.datasource.url=jdbc:tc:postgresql:11-alpine:///databasename" }
)
@ActiveProfiles("test")
public abstract class DockerComposeIntegrationTest {

    @Container
    public static final DockerComposeContainer DOCKER_COMPOSE_CONTAINER;

    static {
        DOCKER_COMPOSE_CONTAINER = new DockerComposeContainer<>(new File("docker/docker-compose.yml"))
            .withLocalCompose(true)
            .withTailChildContainers(true)
            .waitingFor("redis", Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(5)));
        DOCKER_COMPOSE_CONTAINER.start();
    }

}
