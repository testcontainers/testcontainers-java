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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
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
            final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"))
            // }
        ) {

            // startingMongoDBContainer {
            mongoDBContainer.start();
            // }

            final String mongoRsUrl = mongoDBContainer.getReplicaSetUrl();
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
            assertThat(mongoDBContainer.getReplicaSetUrl(databaseName), endsWith(databaseName));
        }
    }

    @Test
    public void shouldTestLoadAndExecuteJsFiles() {
        final String file1 = "mongo_init-1.js";
        final String file2 = "mongo_init-2.js";
        final String targetDir2 = "my-scripts-2/";
        final String targetFile1 = "my-scripts-1/" + file1;
        final String targetFile2 = targetDir2 + file2;
        try (
            final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"))
                .withClasspathResourceMapping(file1, targetFile1, BindMode.READ_ONLY)
                .withClasspathResourceMapping(file2, targetFile2, BindMode.READ_ONLY)
        ) {
            mongoDBContainer.start();
            mongoDBContainer.loadAndExecuteJsFiles(targetFile1, targetDir2);
            final String mongoRsUrl = mongoDBContainer.getReplicaSetUrl();
            assertThat(mongoRsUrl, notNullValue());
            try (
                final MongoClient mongoSyncClient = MongoClients.create(mongoRsUrl)
            ) {
                assertThat(
                    mongoSyncClient.getDatabase("test").getCollection("foo").countDocuments(),
                    is(3L)
                );
                assertThat(
                    mongoSyncClient.getDatabase("test").getCollection("bar").countDocuments(),
                    is(2L)
                );
            }
            assertThatThrownBy(() -> mongoDBContainer.loadAndExecuteJsFiles("non-existed-file"))
                .isExactlyInstanceOf(MongoDBContainer.LoadAndExecuteJsFilesException.class);
        }
    }

    @Test
    public void shouldNotStartBecauseOfDockerEntrypointInitDirectoryIsNotEmpty() {
        try (
            final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"))
                .withClasspathResourceMapping("mongo_init-1.js", "/docker-entrypoint-initdb.d/mongo_init-1.js", BindMode.READ_ONLY)
        ) {
            assertThatThrownBy(mongoDBContainer::start)
                .isExactlyInstanceOf(ContainerLaunchException.class)
                .hasRootCauseExactlyInstanceOf(MongoDBContainer.DockerEntrypointInitDirIsNotEmptyException.class);
        }
    }
}
