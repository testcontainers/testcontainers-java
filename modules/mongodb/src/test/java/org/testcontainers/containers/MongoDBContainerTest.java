package org.testcontainers.containers;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoDBContainerTest extends AbstractMongo {

    /**
     * Taken from <a href="https://docs.mongodb.com/manual/core/transactions/">https://docs.mongodb.com</a>
     */
    @Test
    public void shouldExecuteTransactions() {
        try (
            // creatingMongoDBContainer {
            final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0.10")
            // }
        ) {
            // startingMongoDBContainer {
            mongoDBContainer.start();
            // }
            executeTx(mongoDBContainer);
        }
    }

    @Test
    public void supportsMongoDB_7_0() {
        try (final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")) {
            mongoDBContainer.start();
        }
    }

    @Test
    public void shouldTestDatabaseName() {
        try (final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0.10")) {
            mongoDBContainer.start();
            final String databaseName = "my-db";
            assertThat(mongoDBContainer.getReplicaSetUrl(databaseName)).endsWith(databaseName);
        }
    }
}
