package org.testcontainers.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.MountableFile;

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
    void shouldRunInitScriptWithReplicaSet() {
        try (
            MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0.10")
                .withReplicaSet()
                .withInitScript(MountableFile.forClasspathResource("init-db.js"))
        ) {
            mongoDBContainer.start();

            try (MongoClient client = MongoClients.create(mongoDBContainer.getReplicaSetUrl("testdb"))) {
                Document doc = client.getDatabase("testdb").getCollection("items").find().first();
                assertThat(doc).isNotNull();
                assertThat(doc.getString("name")).isEqualTo("testcontainers");
            }
        }
    }

    @Test
    void shouldRunInitScriptWithoutReplicaSet() {
        try (
            MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.0.10")
                .withInitScript(MountableFile.forClasspathResource("init-db.js"))
        ) {
            mongoDBContainer.start();

            try (MongoClient client = MongoClients.create(mongoDBContainer.getConnectionString())) {
                Document doc = client.getDatabase("testdb").getCollection("items").find().first();
                assertThat(doc).isNotNull();
                assertThat(doc.getString("name")).isEqualTo("testcontainers");
            }
        }
    }
}
