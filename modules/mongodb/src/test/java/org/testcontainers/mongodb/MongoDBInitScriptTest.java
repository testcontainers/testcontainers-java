package org.testcontainers.containers;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MongoDBInitScriptTest {

    @Test
    void testWithInitScript() {
        // Start the container using try-with-resources to ensure it closes automatically
        try (
            MongoDBContainer mongoDB = new MongoDBContainer("mongo:4.0.10")
                // Configure the init script. This triggers a restart inside the container,
                // so the container must wait for the second "waiting for connections" log message.
                .withInitScript("init.js")
                .withStartupTimeout(Duration.ofSeconds(30))
        ) {
            mongoDB.start();

            // Assert that the container started successfully
            assertThat(mongoDB.isRunning()).isTrue();
        }
    }
}
