package org.testcontainers.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class CompatibleImageTest extends AbstractMongo {

    static String[] image() {
        return new String[] {
            "mongo:7",
            "mongodb/mongodb-community-server:7.0.2-ubi8",
            "mongodb/mongodb-enterprise-server:7.0.0-ubi8",
        };
    }

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

    @ParameterizedTest
    @MethodSource("image")
    void shouldSupportSharding(String image) {
        try (MongoDBContainer mongoDBContainer = new MongoDBContainer(image).withSharding()) {
            mongoDBContainer.start();
            final MongoClient mongoClient = MongoClients.create(mongoDBContainer.getReplicaSetUrl());

            mongoClient.getDatabase("mydb1").getCollection("foo").insertOne(new Document("abc", 0));

            Document shards = mongoClient.getDatabase("config").getCollection("shards").find().first();
            assertThat(shards).isNotNull();
            assertThat(shards).isNotEmpty();
            assertThat(isReplicaSet(mongoClient)).isFalse();
        }
    }

    private boolean isReplicaSet(MongoClient mongoClient) {
        return runIsMaster(mongoClient).get("setName") != null;
    }

    private Document runIsMaster(MongoClient mongoClient) {
        return mongoClient.getDatabase("admin").runCommand(new Document("ismaster", 1));
    }
}
