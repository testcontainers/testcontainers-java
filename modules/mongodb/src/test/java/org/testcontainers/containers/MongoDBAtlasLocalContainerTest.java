package org.testcontainers.containers;

import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

public class MongoDBAtlasLocalContainerTest {

    @Test
    public void getConnectionString() {
        try (MongoDBAtlasLocalContainer container = new MongoDBAtlasLocalContainer()) {
            container.start();
            String connectionString = container.getConnectionString();
            assertNotNull(connectionString);
            assertTrue(connectionString.startsWith("mongodb://"));
            assertEquals(String.format("mongodb://localhost:%d/?directConnection=true", container.getFirstMappedPort()), connectionString);
        }
    }

    @Test
    public void createAtlasIndexAndSearchIt() throws Exception {
        try (MongoDBAtlasLocalContainer mongoDAtlas = new MongoDBAtlasLocalContainer()) {
            mongoDAtlas.start();

            String atlasSearchTestcontainersConnectionString = mongoDAtlas.getConnectionString();
            try (AtlasLocalDataAccess atlasLocalDataAccess = new AtlasLocalDataAccess(atlasSearchTestcontainersConnectionString, "test", "test")) {
                atlasLocalDataAccess.initAtlasSearchIndex();

                atlasLocalDataAccess.insertData(new AtlasLocalDataAccess.TestData("test", 123, true));

                AtlasLocalDataAccess.TestData foundRegular = atlasLocalDataAccess.findClassic(123);
                assertNotNull("Failed to find using classic find()", foundRegular);

                //Wait for Atlas Search to index the data (Atlas Search is eventually consistent)
                Instant start = Instant.now();
                AtlasLocalDataAccess.TestData foundSearch = null;
                while (Instant.now().isBefore(start.plusSeconds(5))) {
                    foundSearch = atlasLocalDataAccess.findAtlasSearch(123);
                    if (foundSearch != null) {
                        break;
                    }
                    Thread.sleep(10);
                }
                assertNotNull("Failed to find using Atlas Search", foundSearch);
            }
        }
    }
}
