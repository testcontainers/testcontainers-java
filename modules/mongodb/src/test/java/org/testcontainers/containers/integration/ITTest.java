package org.testcontainers.containers.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDbContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ITTest {
    private final MongoDbContainer mongoDbContainer = new MongoDbContainer(
        //"mongo:4.2.0"
    );

    @BeforeEach
    void setUp() {
        mongoDbContainer.start();
    }

    @AfterEach
    void tearDown() {
        mongoDbContainer.stop();
    }

    @Test
    void shouldTestReplicaSetUrl() {
        assertNotNull(mongoDbContainer.getReplicaSetUrl());
    }
}
