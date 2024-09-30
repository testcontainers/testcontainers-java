package org.testcontainers.mongodb;

import org.junit.Test;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.slf4j.LoggerFactory.getLogger;

public class MongoDBAtlasLocalContainerTest {

    private static final Logger log = getLogger(MongoDBAtlasLocalContainerTest.class);

    @Test
    public void getConnectionString() {
        try (
            MongoDBAtlasLocalContainer container = new MongoDBAtlasLocalContainer("mongodb/mongodb-atlas-local:7.0.9")
        ) {
            container.start();
            String connectionString = container.getConnectionString();
            assertThat(connectionString).isNotNull();
            assertThat(connectionString).startsWith("mongodb://");
            assertThat(connectionString)
                .isEqualTo(
                    String.format(
                        "mongodb://%s:%d/?directConnection=true",
                        container.getHost(),
                        container.getFirstMappedPort()
                    )
                );
        }
    }

    @Test
    public void createAtlasIndexAndSearchIt() throws Exception {
        try (
            // creatingAtlasLocalContainer {
            MongoDBAtlasLocalContainer atlasLocalContainer = new MongoDBAtlasLocalContainer(
                "mongodb/mongodb-atlas-local:7.0.9"
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

                atlasLocalDataAccess.insertData(new AtlasLocalDataAccess.TestData("tests", 123, true));

                Instant start = now();
                log.info(
                    "Waiting for Atlas Search to index the data by polling atlas search query (Atlas Search is eventually consistent)"
                );
                await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(10, TimeUnit.MILLISECONDS)
                    .pollInSameThread()
                    .until(() -> atlasLocalDataAccess.findAtlasSearch("test"), Objects::nonNull);
                log.info(
                    "Atlas Search indexed the new data and was searchable after {}ms.",
                    start.until(now(), ChronoUnit.MILLIS)
                );
            }
        }
    }
}
