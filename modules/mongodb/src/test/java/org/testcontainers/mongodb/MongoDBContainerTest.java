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
                .withInitScript("initEdgeCase!@#%^& *'().js")
                .withStartupTimeout(Duration.ofSeconds(30))
        ) {
            mongoDB.start();

            try (
                com.mongodb.client.MongoClient client = com.mongodb.client.MongoClients.create(
                    mongoDB.getReplicaSetUrl()
                )
            ) {
                String expectedComplexName = "test_col_\"_with_specials_!@#%^&*()";
                String expectedCollectionWithSpecialChars = "col with spaces & symbols !@#";

                com.mongodb.client.MongoDatabase database = client.getDatabase("test");

                assertThat(database.listCollectionNames())
                    .contains(expectedComplexName, expectedCollectionWithSpecialChars);

                com.mongodb.client.MongoCollection<org.bson.Document> collection = database.getCollection(
                    expectedComplexName
                );

                org.bson.Document doc = collection.find(new org.bson.Document("_id", 1)).first();

                assertThat(doc).isNotNull();

                assertThat(doc.getString("key_with_quotes")).isEqualTo("This is a \"double quoted\" string");

                assertThat(doc.getString("key_with_json_chars")).isEqualTo("{ } [ ] : ,");

                assertThat(doc.getString("description"))
                    .isEqualTo("Insertion test for collection with special symbols");
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
    void shouldExecuteInitScriptWithSharding() {
        try (MongoDBContainer mongo = new MongoDBContainer("mongo:7.0.0").withInitScript("init.js").withSharding()) {
            mongo.start();
            assertInitScriptExecuted(mongo);
        }
    }

    private void assertInitScriptExecuted(MongoDBContainer mongo) {
        try (com.mongodb.client.MongoClient client = com.mongodb.client.MongoClients.create(mongo.getReplicaSetUrl())) {
            assertThat(client.getDatabase("test").listCollectionNames()).contains("test_collection");
        }
    }
}
