package org.testcontainers.containers;

import org.junit.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

public class MongoDBAtlasLocalContainerTest {

    @Test
    public void getConnectionString() {
        try (
            MongoDBAtlasLocalContainer container = new MongoDBAtlasLocalContainer(
                MongoDBAtlasLocalContainer.DEFAULT_IMAGE_NAME.withTag("7.0.9")
            )
        ) {
            container.start();
            String connectionString = container.getConnectionString();
            assertThat(connectionString).isNotNull();
            assertThat(connectionString).startsWith("mongodb://");
            assertThat(connectionString).isEqualTo(String.format("mongodb://localhost:%d/?directConnection=true", container.getFirstMappedPort()));
        }
    }

    @Test
    public void createAtlasIndexAndSearchIt() throws Exception {
        try (
            // creatingAtlasLocalContainer {
            MongoDBAtlasLocalContainer atlasLocalContainer = new MongoDBAtlasLocalContainer(
                MongoDBAtlasLocalContainer.DEFAULT_IMAGE_NAME.withTag("7.0.9")
            );
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
                assertThat(foundSearch).isNotNull();
                // }

                AtlasLocalDataAccess.TestData foundRegular = atlasLocalDataAccess.findClassic("tests");
                assertThat(foundRegular).isNotNull();
            }
        }
    }
}
