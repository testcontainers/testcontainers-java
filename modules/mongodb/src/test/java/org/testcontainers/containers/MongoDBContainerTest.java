package org.testcontainers.containers;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.TransactionBody;
import org.bson.Document;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoDBContainerTest {

    /**
     * Taken from <a href="https://docs.mongodb.com/manual/core/transactions/">https://docs.mongodb.com</a>
     */
    @Test
    public void shouldExecuteTransactions() {
        try (
            // creatingMongoDBContainer {
            final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"))
            // }
        ) {
            // startingMongoDBContainer {
            mongoDBContainer.start();
            // }
            executeTx(mongoDBContainer);
        }
    }

    private void executeTx(MongoDBContainer mongoDBContainer) {
        final MongoClient mongoSyncClientBase = MongoClients.create(mongoDBContainer.getConnectionString());
        final MongoClient mongoSyncClient = MongoClients.create(mongoDBContainer.getReplicaSetUrl());
        mongoSyncClient
            .getDatabase("mydb1")
            .getCollection("foo")
            .withWriteConcern(WriteConcern.MAJORITY)
            .insertOne(new Document("abc", 0));
        mongoSyncClient
            .getDatabase("mydb2")
            .getCollection("bar")
            .withWriteConcern(WriteConcern.MAJORITY)
            .insertOne(new Document("xyz", 0));
        mongoSyncClientBase
            .getDatabase("mydb3")
            .getCollection("baz")
            .withWriteConcern(WriteConcern.MAJORITY)
            .insertOne(new Document("def", 0));

        final ClientSession clientSession = mongoSyncClient.startSession();
        final TransactionOptions txnOptions = TransactionOptions
            .builder()
            .readPreference(ReadPreference.primary())
            .readConcern(ReadConcern.LOCAL)
            .writeConcern(WriteConcern.MAJORITY)
            .build();

        final String trxResult = "Inserted into collections in different databases";

        TransactionBody<String> txnBody = () -> {
            final MongoCollection<Document> coll1 = mongoSyncClient.getDatabase("mydb1").getCollection("foo");
            final MongoCollection<Document> coll2 = mongoSyncClient.getDatabase("mydb2").getCollection("bar");

            coll1.insertOne(clientSession, new Document("abc", 1));
            coll2.insertOne(clientSession, new Document("xyz", 999));
            return trxResult;
        };

        try {
            final String trxResultActual = clientSession.withTransaction(txnBody, txnOptions);
            assertThat(trxResultActual).isEqualTo(trxResult);
        } catch (RuntimeException re) {
            throw new IllegalStateException(re.getMessage(), re);
        } finally {
            clientSession.close();
            mongoSyncClient.close();
        }
    }

    @Test
    public void supportsMongoDB_4_4() {
        try (final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.4"))) {
            mongoDBContainer.start();
        }
    }

    @Test
    public void shouldTestDatabaseName() {
        try (final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"))) {
            mongoDBContainer.start();
            final String databaseName = "my-db";
            assertThat(mongoDBContainer.getReplicaSetUrl(databaseName)).endsWith(databaseName);
        }
    }

    @Test
    public void shouldSupportSharding() {
        try (final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6").withSharding()) {
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
