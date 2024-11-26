package org.testcontainers.containers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class CompatibleImageTest extends AbstractMongo {

    private final String image;

    public CompatibleImageTest(String image) {
        this.image = image;
    }

    @Parameterized.Parameters(name = "{0}")
    public static String[] image() {
        return new String[] {
            "mongo:7",
            "mongodb/mongodb-community-server:7.0.2-ubi8",
            "mongodb/mongodb-enterprise-server:7.0.0-ubi8",
        };
    }

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
    public void shouldSupportSharding() {
        try (final MongoDBContainer mongoDBContainer = new MongoDBContainer(this.image).withSharding()) {
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
