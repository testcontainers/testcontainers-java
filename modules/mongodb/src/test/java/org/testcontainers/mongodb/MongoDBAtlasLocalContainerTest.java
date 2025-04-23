package org.testcontainers.mongodb;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class MongoDBAtlasLocalContainerTest {

    private static final Logger log = LoggerFactory.getLogger(MongoDBAtlasLocalContainerTest.class);

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
    public void getDatabaseConnectionString() {
        try (
            MongoDBAtlasLocalContainer container = new MongoDBAtlasLocalContainer("mongodb/mongodb-atlas-local:7.0.9")
        ) {
            container.start();
            String databaseConnectionString = container.getDatabaseConnectionString();
            assertThat(databaseConnectionString).isNotNull();
            assertThat(databaseConnectionString).startsWith("mongodb://");
            assertThat(databaseConnectionString)
                .isEqualTo(
                    String.format(
                        "mongodb://%s:%d/test?directConnection=true",
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

                Instant start = Instant.now();
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
                    start.until(Instant.now(), ChronoUnit.MILLIS)
                );
            }
        }
    }
}
