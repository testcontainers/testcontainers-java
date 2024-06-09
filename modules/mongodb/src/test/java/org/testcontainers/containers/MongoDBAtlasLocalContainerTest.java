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
            assertEquals(String.format("mongodb://localhost:%d", container.getFirstMappedPort()), connectionString);
        }
    }

    @Test
    public void createAtlasIndexAndSearchIt() throws Exception {
        try (
            // creatingAtlasLocalContainer {
            MongoDBAtlasLocalContainer atlasLocalContainer = new MongoDBAtlasLocalContainer()
            // }
        ) {
            // startingAtlasLocalContainer {
            atlasLocalContainer.start();
            // }

            // getConnectionStringAtlasLocalContainer {
            String connectionString = atlasLocalContainer.getConnectionString();
            // }

            try (
                AtlasLocalDataAccess atlasLocalDataAccess = new AtlasLocalDataAccess(connectionString, "test", "test")
            ) {
                atlasLocalDataAccess.initAtlasSearchIndex();

                // writeAndReadBack {
                atlasLocalDataAccess.insertData(new AtlasLocalDataAccess.TestData("tests", 123, true));

                //Wait for Atlas Search to index the data (Atlas Search is eventually consistent)
                Instant start = Instant.now();
                AtlasLocalDataAccess.TestData foundSearch = null;
                while (Instant.now().isBefore(start.plusSeconds(5))) {
                    foundSearch = atlasLocalDataAccess.findAtlasSearch("test");
                    if (foundSearch != null) {
                        break;
                    }
                    Thread.sleep(10);
                }
                assertNotNull("Failed to find using Atlas Search", foundSearch);
                // }

                AtlasLocalDataAccess.TestData foundRegular = atlasLocalDataAccess.findClassic("tests");
                assertNotNull("Failed to find using classic find()", foundRegular);
            }
        }
    }
}
