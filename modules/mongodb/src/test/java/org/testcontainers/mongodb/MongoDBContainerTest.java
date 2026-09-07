package org.testcontainers.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MongoDBContainerTest extends AbstractMongo {

    /**
     * Taken from <a href="https://docs.mongodb.com/manual/core/transactions/">https://docs.mongodb.com</a>
     */
    @Test
    void shouldExecuteTransactions() {
        try (
            // creatingMongoDBContainer {
            MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0.10").withReplicaSet()
            // }
        ) {
            // startingMongoDBContainer {
            mongoDBContainer.start();
            // }
            executeTx(mongoDBContainer);
        }
    }

    @Test
    void supportsMongoDB_7_0() {
        try (MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")) {
            mongoDBContainer.start();
        }
    }

    @Test
    void shouldTestDatabaseName() {
        try (MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0.10")) {
            mongoDBContainer.start();
            final String databaseName = "my-db";
            assertThat(mongoDBContainer.getReplicaSetUrl(databaseName)).endsWith(databaseName);
        }
    }

    @Test
    void shouldRunInitScript() {
        try (
            MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0.10")
                .withCopyFileToContainer(
                    MountableFile.forClasspathResource("mongo-init.js"),
                    "/docker-entrypoint-initdb.d/mongo-init.js"
                )
        ) {
            mongoDBContainer.start();

            try (MongoClient mongoClient = MongoClients.create(mongoDBContainer.getConnectionString())) {
                final Document document = mongoClient
                    .getDatabase("init-script-db")
                    .getCollection("messages")
                    .find()
                    .first();

                assertThat(document).containsEntry("message", "init script ran");
            }
        }
    }

    @Test
    void shouldPreserveCustomWaitStrategy() {
        WaitStrategy customWaitStrategy = Wait
            .forLogMessage("(?i).*waiting for connections.*", 2)
            .withStartupTimeout(Duration.ofMinutes(3));
        try (TestableMongoDBContainer mongoDBContainer = new TestableMongoDBContainer("mongo:4.0.10")) {
            mongoDBContainer
                .withCopyFileToContainer(
                    MountableFile.forClasspathResource("mongo-init.js"),
                    "/docker-entrypoint-initdb.d/mongo-init.js"
                )
                .waitingFor(customWaitStrategy);

            mongoDBContainer.start();
            assertThat(mongoDBContainer.waitStrategy()).isSameAs(customWaitStrategy);
        }
    }

    private static class TestableMongoDBContainer extends MongoDBContainer {

        TestableMongoDBContainer(String dockerImageName) {
            super(dockerImageName);
        }

        WaitStrategy waitStrategy() {
            return getWaitStrategy();
        }
    }
}
