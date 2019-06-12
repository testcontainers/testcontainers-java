package org.testcontainers.containers;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MongoContainerTest {

    public static final Document TEST_DOC = new Document("name", "MongoDB")
        .append("type", "database")
        .append("count", 1)
        .append("versions", Arrays.asList("v3.2", "v3.0", "v2.6"))
        .append("info", new Document("x", 203).append("y", 102));
    public static final String COLLECTION_NAME = "test_collection";

    @Rule
    public MongoContainer mongo = new MongoContainer();

    @Test
    public void testUsage() throws Exception {
        try (MongoClient client = new MongoClient(
            new ServerAddress(
                mongo.getContainerIpAddress(),
                mongo.getMappedPort(MongoContainer.MONGO_PORT)
            ),
            MongoCredential.createCredential("mongo", "admin", "mongo".toCharArray()),
            new MongoClientOptions.Builder().build()
        )) {
            MongoDatabase database = client.getDatabase("testdb");
            database.createCollection(COLLECTION_NAME);

            // Collection is initially empty
            assertEquals(0, database.getCollection(COLLECTION_NAME).countDocuments());

            // Can insert a document
            database.getCollection(COLLECTION_NAME).insertOne(TEST_DOC);
            assertEquals(1, database.getCollection(COLLECTION_NAME).countDocuments());

            Document document = database.getCollection(COLLECTION_NAME).find(eq("name", "MongoDB")).first();
            assertNotNull(document);

            assertEquals("database", document.getString("type"));

            // Can delete a document
            database.getCollection(COLLECTION_NAME).deleteOne(eq("name", "MongoDB"));
            assertEquals(0, database.getCollection(COLLECTION_NAME).countDocuments());
        }
    }

}
