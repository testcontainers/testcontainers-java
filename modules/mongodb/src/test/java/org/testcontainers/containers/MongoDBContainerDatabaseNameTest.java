package org.testcontainers.containers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Slf4j
@DataMongoTest(properties = {"spring.main.banner-mode=off", "data.mongodb.auto-index-creation=true"})
@ContextConfiguration(initializers = MongoDBContainerDatabaseNameTest.Initializer.class)
@RunWith(SpringRunner.class)
public class MongoDBContainerDatabaseNameTest {
    private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer();
    private static final String DATABASE_NAME = "my-db";

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeClass
    public static void setUpAll() {
        MONGO_DB_CONTAINER.start();
    }

    @AfterClass
    public static void tearDownAll() {
        MONGO_DB_CONTAINER.stop();
    }

    @Test
    public void shouldTestDatabaseName() {
        //1. Database name was already set to the MongoTemplate during auto-config.
        assertEquals(DATABASE_NAME, mongoTemplate.getDb().getName());

        try (final MongoClient mongoSyncClient = MongoClients.create(MONGO_DB_CONTAINER.getReplicaSetUrl(DATABASE_NAME))) {
            //2. But the database is not created yet, because we have not performed any writing operation.
            assertFalse(
                "Database: " + DATABASE_NAME + " is supposed to be here",
                isDatabaseInMongoDB(mongoSyncClient, DATABASE_NAME)
            );

            //3. Perform an operation to save a new Product via mongoTemplate.
            mongoTemplate.save(new Product(1L));

            //4. Now the database is created in MongoDB.
            assertTrue(
                "Database: " + DATABASE_NAME + " is supposed to be here",
                isDatabaseInMongoDB(mongoSyncClient, DATABASE_NAME)
            );
        }
    }

    private boolean isDatabaseInMongoDB(
        final MongoClient mongoSyncClient,
        final String databaseName
    ) {
        Objects.requireNonNull(databaseName);
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                mongoSyncClient.listDatabaseNames().iterator(),
                Spliterator.ORDERED
            ),
            false
        ).anyMatch(databaseName::equals);
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NotNull ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                String.format("spring.data.mongodb.uri: %s", MONGO_DB_CONTAINER.getReplicaSetUrl(DATABASE_NAME))
            ).applyTo(configurableApplicationContext);
        }
    }

    @SpringBootApplication
    static class SpringBootApp {
        public static void main(String[] args) {
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Setter(AccessLevel.NONE)
    private static class Product {
        @Indexed(unique = true)
        private Long article;
    }
}
