package org.testcontainers.containers;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.ListSearchIndexesIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.slf4j.LoggerFactory.getLogger;

public class AtlasLocalDataAccess implements AutoCloseable {
    private static final Logger log = getLogger(AtlasLocalDataAccess.class);
    private final MongoClient mongoClient;
    private final MongoCollection<TestData> testCollection;

    public AtlasLocalDataAccess(String connectionString, String databaseName, String collectionName) {
        log.info("DataAccess connecting to {}", connectionString);

        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
            pojoCodecRegistry);
        MongoClientSettings clientSettings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(connectionString))
            .codecRegistry(codecRegistry)
            .build();
        mongoClient = MongoClients.create(clientSettings);
        MongoDatabase testDB = mongoClient.getDatabase(databaseName);
        testDB.createCollection(collectionName);
        testCollection = testDB.getCollection(collectionName, TestData.class);
    }

    @Override
    public void close() throws Exception {
        mongoClient.close();
    }

    public void initAtlasSearchIndex() {
        testCollection.createSearchIndex("AtlasSearchIndex",
                BsonDocument.parse("{\"mappings\": " +
                    "{" +
                        "\"dynamic\": false," +
                        "\"fields\": " +
                            "{" +
                                "\"test2\": {\"type\": \"number\",\"representation\": \"int64\",\"indexDoubles\": false}," +
                                "\"test\": {\"type\": \"string\"}," +
                                "\"test3\": {\"type\": \"boolean\"}" +
                            "}" +
                    "}" +
                "}")
        );

        //wait for the search index to be ready
        boolean ready = false;
        while (!ready) {
            ListSearchIndexesIterable<Document> searchIndexes = testCollection.listSearchIndexes();
            for (Document searchIndex : searchIndexes) {
                if (searchIndex.get("name").equals("AtlasSearchIndex")) {
                    ready = searchIndex.get("status").equals("READY");
                    if (ready) {
                        System.out.println("Search index AtlasSearchIndex is ready");
                        break;
                    }
                }
            }
        }
    }

    public void insertData(TestData data) {
        log.info("Inserting document {}", data);
        testCollection.insertOne(data);
    }

    public TestData findClassic(int test2) {
        return testCollection.find(eq("test2", test2)).first();
    }

    public TestData findAtlasSearch(int test2) {
        List<Document> query = Arrays.asList(new Document("$search",
                        new Document("index", "AtlasSearchIndex")
                                .append("equals",
                                        new Document()
                                                .append("path", "test2")
                                                .append("value", test2)
                                )
                )
        );
        return testCollection.aggregate(query).first();
    }


    public static class TestData{
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
    }

}
