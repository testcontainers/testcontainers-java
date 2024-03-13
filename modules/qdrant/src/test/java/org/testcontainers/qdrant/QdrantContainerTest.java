package org.testcontainers.qdrant;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.QdrantOuterClass;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

public class QdrantContainerTest {

    @Test
    public void test() throws ExecutionException, InterruptedException {
        try (
            // qdrantContainer {
            QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:v1.7.4")
            // }
        ) {
            qdrant.start();

            QdrantClient client = new QdrantClient(
                QdrantGrpcClient.newBuilder(qdrant.getHost(), qdrant.getGrpcPort(), false).build()
            );
            QdrantOuterClass.HealthCheckReply healthCheckReply = client.healthCheckAsync().get();
            assertThat(healthCheckReply.getVersion()).isEqualTo("1.7.4");

            client.close();
        }
    }

    @Test
    public void testApiKey() throws ExecutionException, InterruptedException {
        String apiKey = UUID.randomUUID().toString();
        try (QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:v1.7.4").withApiKey(apiKey);) {
            qdrant.start();

            final QdrantClient unauthClient = new QdrantClient(
                QdrantGrpcClient.newBuilder(qdrant.getHost(), qdrant.getGrpcPort(), false).build()
            );

            assertThrows(ExecutionException.class, () -> unauthClient.healthCheckAsync().get());

            unauthClient.close();

            final QdrantClient client = new QdrantClient(
                QdrantGrpcClient.newBuilder(qdrant.getHost(), qdrant.getGrpcPort(), false).withApiKey(apiKey).build()
            );

            QdrantOuterClass.HealthCheckReply healthCheckReply = client.healthCheckAsync().get();
            assertThat(healthCheckReply.getVersion()).isEqualTo("1.7.4");

            client.close();
        }
    }
}
