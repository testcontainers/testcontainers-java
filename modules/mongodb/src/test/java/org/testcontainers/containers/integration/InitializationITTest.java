package org.testcontainers.containers.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDbContainer;
import org.testcontainers.containers.core.IntegrationTest;

@IntegrationTest
class InitializationITTest extends BaseInitializationITTest {
    // creatingMongoDbContainer {
    private final MongoDbContainer mongoDbContainer =
        new MongoDbContainer("mongo:4.2.0");
    // }

    // startingStoppingMongoDbContainer {
    @BeforeEach
    void setUp() {
        mongoDbContainer.start();
    }

    @AfterEach
    void tearDown() {
        mongoDbContainer.stop();
    }
    // }

    @Test
    void shouldTestRsStatus() {
        super.shouldTestRsStatus(mongoDbContainer);
    }

    @Test
    void shouldTestVersionAndDockerImageName() {
        super.shouldTestVersionAndDockerImageName(mongoDbContainer);
    }
}
