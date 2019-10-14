package org.testcontainers.containers.integration;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.SneakyThrows;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import org.testcontainers.containers.MongoDbContainer;
import org.testcontainers.containers.util.SubscriberHelperUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

abstract class BaseInitializationITTest {

    void shouldTestRsStatus(
        final MongoDbContainer mongoDbContainer
    ) {
        // GIVEN
        final String replicaSetUrl = mongoDbContainer.getReplicaSetUrl();
        assertNotNull(replicaSetUrl);

        final MongoClient mongoReactiveClient = MongoClients.create(replicaSetUrl);
        final MongoDatabase db = mongoReactiveClient.getDatabase("admin");

        // WHEN + THEN
        try {
            final SubscriberHelperUtils.PrintDocumentSubscriber subscriber = getSubscriber(
                db.runCommand(new Document("replSetGetStatus", 1))
            );
            final Document document = getDocument(subscriber.getReceived());
            assertEquals(Double.valueOf("1"), document.get("ok", Double.class));
            final List<Document> mongoNodesActual =
                document.getList("members", Document.class);
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

    void shouldTestVersionAndDockerImageName(
        final MongoDbContainer mongoDbContainer
    ) {
        // GIVEN
        final String replicaSetUrl = mongoDbContainer.getReplicaSetUrl();
        assertNotNull(replicaSetUrl);
        final String dockerImageName = mongoDbContainer.getDockerImageName();
        assertNotNull(dockerImageName);
        final MongoClient mongoClient = MongoClients.create(replicaSetUrl);
        final MongoDatabase db = mongoClient.getDatabase("test");

        // WHEN + THEN
        try {
            final SubscriberHelperUtils.PrintDocumentSubscriber subscriber = getSubscriber(
                db.runCommand(new Document("buildInfo", 1))
            );
            final String version = getDocument(subscriber.getReceived()).getString("version");
            final String versionExpected = dockerImageName.substring(dockerImageName.indexOf(":") + 1);
            assertEquals(versionExpected, version);
        } finally {
            mongoClient.close();
        }
    }

    @SneakyThrows
    private SubscriberHelperUtils.PrintDocumentSubscriber getSubscriber(
        Publisher<Document> command
    ) {
        final SubscriberHelperUtils.PrintDocumentSubscriber subscriber =
            new SubscriberHelperUtils.PrintDocumentSubscriber();
        command.subscribe(subscriber);
        subscriber.await();
        return subscriber;
    }
}
