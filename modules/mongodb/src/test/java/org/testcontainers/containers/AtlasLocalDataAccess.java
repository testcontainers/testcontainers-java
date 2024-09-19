package org.testcontainers.containers;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.ListSearchIndexesIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import lombok.Setter;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Aggregates.search;
import static com.mongodb.client.model.search.SearchOperator.of;
import static com.mongodb.client.model.search.SearchOperator.text;
import static com.mongodb.client.model.search.SearchOptions.searchOptions;
import static com.mongodb.client.model.search.SearchPath.fieldPath;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.awaitility.Awaitility.await;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.slf4j.LoggerFactory.getLogger;

public class AtlasLocalDataAccess implements AutoCloseable {

    private static final Logger log = getLogger(AtlasLocalDataAccess.class);
    private final MongoClient mongoClient;
    private final MongoDatabase testDB;
    private final MongoCollection<TestData> testCollection;
    private final String collectionName;

    public AtlasLocalDataAccess(String connectionString, String databaseName, String collectionName) {
        this.collectionName = collectionName;
        log.info("DataAccess connecting to {}", connectionString);

        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
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
            Files.readAllBytes(Paths.get(requireNonNull(getClass().getResource("/atlas-local-index.json")).toURI())),
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
            .until(
                this::getIndexStatus,
                "READY"::equalsIgnoreCase);

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
        Bson searchClause = search(
            of(text(fieldPath("test"), test).fuzzy()),
            searchOptions().index("AtlasSearchIndex")
        );
        log.trace(
            "Searching for document using Atlas Search:\n{}",
            searchClause.toBsonDocument().toJson(JsonWriterSettings.builder().indent(true).build())
        );
        return testCollection.aggregate(singletonList(searchClause)).first();
    }

    @Setter
    @Getter
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

        @Override
        public String toString() {
            return "TestData{" + "test='" + test + '\'' + ", test2=" + test2 + ", test3=" + test3 + '}';
        }
    }
}
