package org.testcontainers.containers.integration;

import com.mongodb.reactivestreams.client.MongoClients;
import lombok.SneakyThrows;
import lombok.val;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.testcontainers.containers.MongoDbContainer;
import org.testcontainers.containers.core.IntegrationTest;
import org.testcontainers.containers.util.SubscriberHelperUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@IntegrationTest
class InitializationITTest {
    private final MongoDbContainer mongoDbContainer =
        new MongoDbContainer("mongo:4.2.0");

    @BeforeEach
    void setUp() {
        mongoDbContainer.start();
    }

    @AfterEach
    void tearDown() {
        mongoDbContainer.stop();
    }

    @Test
    void shouldTestRsStatus() {
        // GIVEN
        val replicaSetUrl = mongoDbContainer.getReplicaSetUrl();
        assertNotNull(replicaSetUrl);

        val mongoReactiveClient = MongoClients.create(replicaSetUrl);
        val db = mongoReactiveClient.getDatabase("admin");

        // WHEN + THEN
        try {
            val subscriber = getSubscriber(
                db.runCommand(new Document("replSetGetStatus", 1))
            );
            val document = getDocument(subscriber.getReceived());
            assertEquals(Double.valueOf("1"), document.get("ok", Double.class));
            val mongoNodesActual = document.getList("members", Document.class);
            assertEquals(1, mongoNodesActual.size());
            assertEquals(1, mongoNodesActual.get(0).getInteger("state"));
        } finally {
            mongoReactiveClient.close();
        }
    }

    @NotNull
    private Document getDocument(List<Document> documents) {
        return documents.get(0);
    }

    @Test
    void shouldTestVersionAndDockerImageName() {
        // GIVEN
        val replicaSetUrl = mongoDbContainer.getReplicaSetUrl();
        assertNotNull(replicaSetUrl);
        val dockerImageName = mongoDbContainer.getDockerImageName();
        assertNotNull(dockerImageName);
        val mongoClient = MongoClients.create(replicaSetUrl);
        val db = mongoClient.getDatabase("test");

        // WHEN + THEN
        try {
            val subscriber = getSubscriber(
                db.runCommand(new Document("buildInfo", 1))
            );
            val version = getDocument(subscriber.getReceived()).getString("version");
            val versionExpected =
                dockerImageName.substring(dockerImageName.indexOf(":") + 1);
            assertEquals(
                versionExpected,
                version
            );
        } finally {
            mongoClient.close();
        }
    }

    @SneakyThrows
    private SubscriberHelperUtils.PrintDocumentSubscriber getSubscriber(
        Publisher<Document> command
    ) {
        val subscriber = new SubscriberHelperUtils.PrintDocumentSubscriber();
        command.subscribe(subscriber);
        subscriber.await();
        return subscriber;
    }
}
