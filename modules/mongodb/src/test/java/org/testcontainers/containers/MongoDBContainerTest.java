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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;


public class MongoDBContainerTest {

    /**
     * Taken from <a href="https://docs.mongodb.com/manual/core/transactions/">https://docs.mongodb.com</a>
     */
    @Test
    public void shouldExecuteTransactions() {
        try (
            // creatingMongoDBContainer {
            final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10")).withReplicaSet()
            // }
        ) {

            // startingMongoDBContainer {
            mongoDBContainer.start();
            // }

            final String mongoRsUrl = mongoDBContainer.getConnectionUri();
            assertNotNull(mongoRsUrl);
            final MongoClient mongoSyncClient = MongoClients.create(mongoRsUrl);
            mongoSyncClient.getDatabase("mydb1").getCollection("foo")
                .withWriteConcern(WriteConcern.MAJORITY).insertOne(new Document("abc", 0));
            mongoSyncClient.getDatabase("mydb2").getCollection("bar")
                .withWriteConcern(WriteConcern.MAJORITY).insertOne(new Document("xyz", 0));

            final ClientSession clientSession = mongoSyncClient.startSession();
            final TransactionOptions txnOptions = TransactionOptions.builder()
                .readPreference(ReadPreference.primary())
                .readConcern(ReadConcern.LOCAL)
                .writeConcern(WriteConcern.MAJORITY)
                .build();

            final String trxResult = "Inserted into collections in different databases";

            TransactionBody<String> txnBody = () -> {
                final MongoCollection<Document> coll1 =
                    mongoSyncClient.getDatabase("mydb1").getCollection("foo");
                final MongoCollection<Document> coll2 =
                    mongoSyncClient.getDatabase("mydb2").getCollection("bar");

                coll1.insertOne(clientSession, new Document("abc", 1));
                coll2.insertOne(clientSession, new Document("xyz", 999));
                return trxResult;
            };

            try {
                final String trxResultActual = clientSession.withTransaction(txnBody, txnOptions);
                assertEquals(trxResult, trxResultActual);
            } catch (RuntimeException re) {
                throw new IllegalStateException(re.getMessage(), re);
            } finally {
                clientSession.close();
                mongoSyncClient.close();
            }
        }
    }

    @Test
    public void supportsMongoDB_4_4() {
        try (
            final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.4"))
        ) {
            mongoDBContainer.start();
        }
    }

    @Test
    public void shouldTestDatabaseName() {
        try (
            final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"))
        ) {
            mongoDBContainer.start();
            final String databaseName = "my-db";
            assertThat(mongoDBContainer.getConnectionUri(databaseName), endsWith(databaseName));
        }
    }

    @Test
    public void shouldTestAuth() {
        try (
            final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10")).withAuth()
        ) {
            mongoDBContainer.start();

            //Basic We connected test.
            final MongoClient mongoSyncClient = MongoClients.create(mongoDBContainer.getConnectionUri());
            assertNotNull(mongoSyncClient.listDatabaseNames().first());
        }
    }

    @Test
    public void shouldTestAuthAndReplica() {
        try (
            final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"))
                .withReplicaSet()
                .withAuth()
        ) {
            mongoDBContainer.start();

            //Basic We connected test.
            final MongoClient mongoSyncClient = MongoClients.create(mongoDBContainer.getConnectionUri());
            assertNotNull(mongoSyncClient.listDatabaseNames().first());
        }
    }


    @Test
    public void shouldTestAuthOverrideUNAndPW() {
        try (
            final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"))
                .withAuth("testUser", "testPassword")
        ) {
            mongoDBContainer.start();

            assertThat(mongoDBContainer.getConnectionUri(), containsString("testUser:testPassword@"));
            //Basic We connected test.
            final MongoClient mongoSyncClient = MongoClients.create(mongoDBContainer.getConnectionUri());
            assertNotNull(mongoSyncClient.listDatabaseNames().first());
        }
    }
}
