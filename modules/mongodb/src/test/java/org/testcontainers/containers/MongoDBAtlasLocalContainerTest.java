package org.testcontainers.containers;

import org.junit.Test;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

public class MongoDBAtlasLocalContainerTest {

    @Test
    public void getConnectionString() {
        try (
            MongoDBAtlasLocalContainer container = new MongoDBAtlasLocalContainer("mongodb/mongodb-atlas-local:7.0.9")
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
            MongoDBAtlasLocalContainer atlasLocalContainer =
                new MongoDBAtlasLocalContainer("mongodb/mongodb-atlas-local:7.0.9");
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

                atlasLocalDataAccess.insertData(new AtlasLocalDataAccess.TestData("tests", 123, true));

                //Wait for Atlas Search to index the data (Atlas Search is eventually consistent)
                await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(10, TimeUnit.MILLISECONDS)
                    .pollInSameThread()
                    .until(
                        () -> atlasLocalDataAccess.findAtlasSearch("test"),
                        Objects::nonNull);

                AtlasLocalDataAccess.TestData foundRegular = atlasLocalDataAccess.findClassic("tests");
                assertThat(foundRegular).isNotNull();
            }
        }
    }
}
