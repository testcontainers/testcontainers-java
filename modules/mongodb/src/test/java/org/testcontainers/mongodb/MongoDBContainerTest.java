package org.testcontainers.mongodb;

import org.junit.jupiter.api.Test;

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
    void shouldExecuteInitScript() {
        try (
            MongoDBContainer mongoDB = new MongoDBContainer("mongo:4.0.10")
                .withInitScript("init.js")
                .withStartupTimeout(Duration.ofSeconds(30))
        ) {
            mongoDB.start();
            assertThat(mongoDB.isRunning()).isTrue();
        }
    }

    @Test
    void shouldExecuteInitScriptWithEdgeCases() {
        try (
            MongoDBContainer mongoDB = new MongoDBContainer("mongo:4.0.10")
                .withInitScript("initEdgeCase.js")
                .withEnv("LANG", "C.UTF-8")
                .withEnv("LC_ALL", "C.UTF-8")
                .withStartupTimeout(Duration.ofSeconds(30))
        ) {
            mongoDB.start();

            try (
                com.mongodb.client.MongoClient client = com.mongodb.client.MongoClients.create(
                    mongoDB.getReplicaSetUrl()
                )
            ) {
                String expectedComplexName = "test_col_\"_with_specials_!@#%^&*()";
                String expectedJapaneseName = "日本語 コレクション ﾃｽﾄ";

                assertThat(client.getDatabase("test").listCollectionNames())
                    .as("Check if init script created the collections with special chars and Japanese")
                    .contains(expectedComplexName, expectedJapaneseName);
            }
        }
    }

    @Test
    void shouldExecuteInitScriptWithReplicaSet() {
        try (MongoDBContainer mongo = new MongoDBContainer("mongo:7.0.0").withInitScript("init.js").withReplicaSet()) {
            mongo.start();
            assertInitScriptExecuted(mongo);
        }
    }

    @Test
    void shouldExecuteInitScriptWithReplicaSetConfiguredFirst() {
        try (MongoDBContainer mongo = new MongoDBContainer("mongo:7.0.0").withReplicaSet().withInitScript("init.js")) {
            mongo.start();
            assertInitScriptExecuted(mongo);
        }
    }

    @Test
    void shouldExecuteInitScriptWithSharding() {
        try (MongoDBContainer mongo = new MongoDBContainer("mongo:7.0.0").withInitScript("init.js").withSharding()) {
            mongo.start();
            assertInitScriptExecuted(mongo);
        }
    }

    @Test
    void shouldExecuteInitScriptWithShardingConfiguredFirst() {
        try (MongoDBContainer mongo = new MongoDBContainer("mongo:7.0.0").withSharding().withInitScript("init.js")) {
            mongo.start();
            assertInitScriptExecuted(mongo);
        }
    }

    private void assertInitScriptExecuted(MongoDBContainer mongo) {
        try (com.mongodb.client.MongoClient client = com.mongodb.client.MongoClients.create(mongo.getReplicaSetUrl())) {
            assertThat(client.getDatabase("test").listCollectionNames())
                .as("Check if init.js created the collection")
                .contains("test_collection");
        }
    }
}
