package com.example;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DemoApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    static {
        GenericContainer redis = new GenericContainer("redis:3-alpine")
            .withExposedPorts(6379);
        redis.start();

        PostgreSQLContainer postgreSQLContainer = (PostgreSQLContainer) new PostgreSQLContainer("postgres:11-alpine")
            .withDatabaseName("demo")
            .withUsername("demouser")
            .withPassword("demopass")
            .withStartupTimeout(Duration.ofSeconds(600));
        postgreSQLContainer.start();

        System.setProperty("spring.redis.host", redis.getContainerIpAddress());
        System.setProperty("spring.redis.port", redis.getFirstMappedPort() + "");
        System.setProperty("spring.datasource.url", postgreSQLContainer.getJdbcUrl());

    }
}
