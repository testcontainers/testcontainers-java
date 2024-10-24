package org.testcontainers.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.ListSearchIndexesIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.search.SearchOperator;
import com.mongodb.client.model.search.SearchOptions;
import com.mongodb.client.model.search.SearchPath;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class AtlasLocalDataAccess implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AtlasLocalDataAccess.class);

    private final MongoClient mongoClient;

    private final MongoDatabase testDB;

    private final MongoCollection<TestData> testCollection;

    private final String collectionName;

    public AtlasLocalDataAccess(String connectionString, String databaseName, String collectionName) {
        this.collectionName = collectionName;
        log.info("DataAccess connecting to {}", connectionString);

        CodecRegistry pojoCodecRegistry = CodecRegistries.fromProviders(
            PojoCodecProvider.builder().automatic(true).build()
        );
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            pojoCodecRegistry
        );
        MongoClientSettings clientSettings = MongoClientSettings
            .builder()
            .applyConnectionString(new ConnectionString(connectionString))
            .codecRegistry(codecRegistry)
            .build();
        mongoClient = MongoClients.create(clientSettings);
        testDB = mongoClient.getDatabase(databaseName);
        testCollection = testDB.getCollection(collectionName, TestData.class);
    }

    @Override
    public void close() {
        mongoClient.close();
    }

    public void initAtlasSearchIndex() throws URISyntaxException, IOException, InterruptedException {
        //Create the collection (if it doesn't exist). Required because unlike other database operations, createSearchIndex will fail if the collection doesn't exist yet
        testDB.createCollection(collectionName);

        //Read the atlas search index JSON from a resource file
        String atlasSearchIndexJson = new String(
            Files.readAllBytes(Paths.get(getClass().getResource("/atlas-local-index.json").toURI())),
            StandardCharsets.UTF_8
        );
        log.info(
            "Creating Atlas Search index AtlasSearchIndex on collection {}:\n{}",
            collectionName,
            atlasSearchIndexJson
        );
        testCollection.createSearchIndex("AtlasSearchIndex", BsonDocument.parse(atlasSearchIndexJson));

        //wait for the atlas search index to be ready
        Instant start = Instant.now();
        await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .pollInSameThread()
            .until(this::getIndexStatus, "READY"::equalsIgnoreCase);

        log.info(
            "Atlas Search index AtlasSearchIndex on collection {} is ready (took {} milliseconds) to create.",
            collectionName,
            start.until(Instant.now(), ChronoUnit.MILLIS)
        );
    }

    private String getIndexStatus() {
        ListSearchIndexesIterable<Document> searchIndexes = testCollection.listSearchIndexes();
        for (Document searchIndex : searchIndexes) {
            if (searchIndex.get("name").equals("AtlasSearchIndex")) {
                return searchIndex.getString("status");
            }
        }
        return null;
    }

    public void insertData(TestData data) {
        log.info("Inserting document {}", data);
        testCollection.insertOne(data);
    }

    public TestData findAtlasSearch(String test) {
        Bson searchClause = Aggregates.search(
            SearchOperator.of(SearchOperator.text(SearchPath.fieldPath("test"), test).fuzzy()),
            SearchOptions.searchOptions().index("AtlasSearchIndex")
        );
        log.trace(
            "Searching for document using Atlas Search:\n{}",
            searchClause.toBsonDocument().toJson(JsonWriterSettings.builder().indent(true).build())
        );
        return testCollection.aggregate(Collections.singletonList(searchClause)).first();
    }

    public static class TestData {

        String test;

        int test2;

        boolean test3;

        public TestData() {}

        public TestData(String test, int test2, boolean test3) {
            this.test = test;
            this.test2 = test2;
            this.test3 = test3;
        }

        public String getTest() {
            return test;
        }

        public void setTest(String test) {
            this.test = test;
        }

        public int getTest2() {
            return test2;
        }

        public void setTest2(int test2) {
            this.test2 = test2;
        }

        public boolean isTest3() {
            return test3;
        }

        public void setTest3(boolean test3) {
            this.test3 = test3;
        }

        @Override
        public String toString() {
            return "TestData{" + "test='" + test + '\'' + ", test2=" + test2 + ", test3=" + test3 + '}';
        }
    }
}
