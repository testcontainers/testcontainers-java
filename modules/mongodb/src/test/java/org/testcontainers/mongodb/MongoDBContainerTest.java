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
    @SuppressWarnings("OctalInteger")
    void shouldRunShInitScript() {
        try (
            MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0.10")
                .withCopyFileToContainer(
                    MountableFile.forClasspathResource("mongo-init.sh", 0777),
                    "/docker-entrypoint-initdb.d/mongo-init.sh"
                )
        ) {
            mongoDBContainer.start();

            try (MongoClient mongoClient = MongoClients.create(mongoDBContainer.getConnectionString())) {
                final Document document = mongoClient
                    .getDatabase("init-script-sh-db")
                    .getCollection("messages")
                    .find()
                    .first();

                assertThat(document).containsEntry("message", "init sh script ran");
            }
        }
    }

    @Test
    void shouldNotTriggerTwoOccurrenceWaitForUnrelatedFile() {
        try (TestableMongoDBContainer mongoDBContainer = new TestableMongoDBContainer("mongo:4.0.10")) {
            WaitStrategy defaultWaitStrategy = mongoDBContainer.waitStrategy();
            mongoDBContainer.withCopyFileToContainer(
                MountableFile.forClasspathResource("mongo-init.js"),
                "/docker-entrypoint-initdb.d/notes.txt"
            );
            mongoDBContainer.triggerContainerIsStarting();
            assertThat(mongoDBContainer.waitStrategy()).isSameAs(defaultWaitStrategy);
        }
    }

    @Test
    void shouldNotTriggerTwoOccurrenceWaitForNestedPath() {
        try (TestableMongoDBContainer mongoDBContainer = new TestableMongoDBContainer("mongo:4.0.10")) {
            WaitStrategy defaultWaitStrategy = mongoDBContainer.waitStrategy();
            mongoDBContainer.withCopyFileToContainer(
                MountableFile.forClasspathResource("mongo-init.js"),
                "/docker-entrypoint-initdb.d/nested/mongo-init.js"
            );
            mongoDBContainer.triggerContainerIsStarting();
            assertThat(mongoDBContainer.waitStrategy()).isSameAs(defaultWaitStrategy);
        }
    }

    @Test
    void shouldPreserveCustomWaitStrategyViaWaitingFor() {
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

    @Test
    void shouldPreserveCustomWaitStrategyViaSetWaitStrategy() {
        WaitStrategy customWaitStrategy = Wait
            .forLogMessage("(?i).*waiting for connections.*", 2)
            .withStartupTimeout(Duration.ofMinutes(3));
        try (TestableMongoDBContainer mongoDBContainer = new TestableMongoDBContainer("mongo:4.0.10")) {
            mongoDBContainer.withCopyFileToContainer(
                MountableFile.forClasspathResource("mongo-init.js"),
                "/docker-entrypoint-initdb.d/mongo-init.js"
            );
            mongoDBContainer.setWaitStrategy(customWaitStrategy);

            mongoDBContainer.start();
            assertThat(mongoDBContainer.waitStrategy()).isSameAs(customWaitStrategy);
        }
    }

    @Test
    void shouldTriggerTwoOccurrenceWaitForDirectoryMount() {
        try (TestableMongoDBContainer mongoDBContainer = new TestableMongoDBContainer("mongo:4.0.10")) {
            WaitStrategy defaultWaitStrategy = mongoDBContainer.waitStrategy();
            mongoDBContainer.withFileSystemBind("/dummy/host/dir", "/docker-entrypoint-initdb.d");
            mongoDBContainer.triggerContainerIsStarting();
            assertThat(mongoDBContainer.waitStrategy()).isNotSameAs(defaultWaitStrategy);
        }
    }

    @Test
    void shouldTriggerTwoOccurrenceWaitOnlyForValidInitScriptPaths() {
        assertThat(triggersTwoOccurrenceWaitForBind("/docker-entrypoint-initdb.d")).isTrue();
        assertThat(triggersTwoOccurrenceWaitForBind("/docker-entrypoint-initdb.d/")).isTrue();
        assertThat(triggersTwoOccurrenceWaitForBind("/docker-entrypoint-initdb.d/init.js")).isTrue();
        assertThat(triggersTwoOccurrenceWaitForBind("/docker-entrypoint-initdb.d/init.sh")).isTrue();
        assertThat(triggersTwoOccurrenceWaitForBind("/docker-entrypoint-initdb.d/nested/init.js")).isFalse();
        assertThat(triggersTwoOccurrenceWaitForBind("/docker-entrypoint-initdb.d/notes.txt")).isFalse();

        assertThat(triggersTwoOccurrenceWaitForCopy("/docker-entrypoint-initdb.d")).isTrue();
        assertThat(triggersTwoOccurrenceWaitForCopy("/docker-entrypoint-initdb.d/")).isTrue();
        assertThat(triggersTwoOccurrenceWaitForCopy("/docker-entrypoint-initdb.d/mongo-init.js")).isTrue();
        assertThat(triggersTwoOccurrenceWaitForCopy("/docker-entrypoint-initdb.d/mongo-init.sh")).isTrue();
        assertThat(triggersTwoOccurrenceWaitForCopy("/docker-entrypoint-initdb.d/nested/mongo-init.js")).isFalse();
        assertThat(triggersTwoOccurrenceWaitForCopy("/docker-entrypoint-initdb.d/notes.txt")).isFalse();
        assertThat(triggersTwoOccurrenceWaitForCopy("/docker-entrypoint-initdb.d/data.json")).isFalse();
        assertThat(triggersTwoOccurrenceWaitForCopy("/docker-entrypoint-initdb.d/.gitkeep")).isFalse();
        assertThat(triggersTwoOccurrenceWaitForCopy("/docker-entrypoint-initdb.d/.js")).isFalse();
        assertThat(triggersTwoOccurrenceWaitForCopy("/other-dir/mongo-init.js")).isFalse();
    }

    private boolean triggersTwoOccurrenceWaitForBind(String containerPath) {
        try (TestableMongoDBContainer container = new TestableMongoDBContainer("mongo:4.0.10")) {
            WaitStrategy defaultWaitStrategy = container.waitStrategy();
            container.withFileSystemBind("/dummy", containerPath);
            container.triggerContainerIsStarting();
            return container.waitStrategy() != defaultWaitStrategy;
        }
    }

    private boolean triggersTwoOccurrenceWaitForCopy(String containerPath) {
        try (TestableMongoDBContainer container = new TestableMongoDBContainer("mongo:4.0.10")) {
            WaitStrategy defaultWaitStrategy = container.waitStrategy();
            container.withCopyFileToContainer(MountableFile.forClasspathResource("mongo-init.js"), containerPath);
            container.triggerContainerIsStarting();
            return container.waitStrategy() != defaultWaitStrategy;
        }
    }

    private static class TestableMongoDBContainer extends MongoDBContainer {

        TestableMongoDBContainer(String dockerImageName) {
            super(dockerImageName);
        }

        WaitStrategy waitStrategy() {
            return getWaitStrategy();
        }

        void triggerContainerIsStarting() {
            containerIsStarting(null);
        }
    }
}
