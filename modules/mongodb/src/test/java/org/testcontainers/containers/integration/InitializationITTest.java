package org.testcontainers.containers.integration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.MongoDbContainer;

public class InitializationITTest extends BaseInitializationITTest {
    // creatingMongoDbContainer {
    private final MongoDbContainer mongoDbContainer =
        new MongoDbContainer("mongo:4.2.0");
    // }

    // startingStoppingMongoDbContainer {
    @Before
    public void setUp() {
        mongoDbContainer.start();
    }

    @After
    public void tearDown() {
        mongoDbContainer.stop();
    }
    // }

    @Test
    public void shouldTestRsStatus() {
        super.shouldTestRsStatus(mongoDbContainer);
    }

    @Test
    public void shouldTestVersionAndDockerImageName() {
        super.shouldTestVersionAndDockerImageName(mongoDbContainer);
    }
}
