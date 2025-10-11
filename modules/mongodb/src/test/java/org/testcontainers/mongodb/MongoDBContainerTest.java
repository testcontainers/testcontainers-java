package org.testcontainers.containers;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MongoDBContainerTest extends AbstractMongo {

    /**
     * Taken from <a href="https://docs.mongodb.com/manual/core/transactions/">https://docs.mongodb.com</a>
     */
    @Test
    void shouldExecuteTransactions() {
        try (
            // creatingMongoDBContainer {
            final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0.10")
            // }
        ) {
            // startingMongoDBContainer {
            mongoDBContainer.start();
            // }
            Assertions.assertThatNoException().isThrownBy(() -> executeTx(mongoDBContainer));
        }
    }

    @Test
    void supportsMongoDB_7_0() {
        Assertions
            .assertThatNoException()
            .isThrownBy(() -> {
                try (final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")) {
                    mongoDBContainer.start();
                }
            });
    }

    @Test
    void shouldTestDatabaseName() {
        try (final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0.10")) {
            mongoDBContainer.start();
            final String databaseName = "my-db";
            assertThat(mongoDBContainer.getReplicaSetUrl(databaseName)).endsWith(databaseName);
        }
    }
}
