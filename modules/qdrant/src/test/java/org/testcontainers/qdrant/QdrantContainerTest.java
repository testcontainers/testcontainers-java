package org.testcontainers.qdrant;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.QdrantOuterClass;
import org.junit.jupiter.api.Test;
import org.testcontainers.images.builder.Transferable;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QdrantContainerTest {

    @Test
    void shouldReturnVersion() throws ExecutionException, InterruptedException {
        try (
            // qdrantContainer {
            QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:v1.7.4")
            // }
        ) {
            qdrant.start();

            try (
                QdrantClient client = new QdrantClient(
                    QdrantGrpcClient.newBuilder(qdrant.getHost(), qdrant.getGrpcPort(), false).build()
                )
            ) {
                QdrantOuterClass.HealthCheckReply healthCheckReply = client.healthCheckAsync().get();
                assertThat(healthCheckReply.getVersion()).isEqualTo("1.7.4");
            }
        }
    }

    @Test
    void shouldSetApiKey() throws ExecutionException, InterruptedException {
        String apiKey = UUID.randomUUID().toString();
        try (QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:v1.7.4").withApiKey(apiKey)) {
            qdrant.start();

            try (
                QdrantClient unauthClient = new QdrantClient(
                    QdrantGrpcClient.newBuilder(qdrant.getHost(), qdrant.getGrpcPort(), false).build()
                )
            ) {
                assertThatThrownBy(() -> unauthClient.healthCheckAsync().get()).isInstanceOf(ExecutionException.class);
            }

            try (
                QdrantClient client = new QdrantClient(
                    QdrantGrpcClient
                        .newBuilder(qdrant.getHost(), qdrant.getGrpcPort(), false)
                        .withApiKey(apiKey)
                        .build()
                )
            ) {
                QdrantOuterClass.HealthCheckReply healthCheckReply = client.healthCheckAsync().get();
                assertThat(healthCheckReply.getVersion()).isEqualTo("1.7.4");
            }
        }
    }

    @Test
    void shouldSetApiKeyUsingConfigFile() throws ExecutionException, InterruptedException {
        String apiKey = UUID.randomUUID().toString();
        String configFile = "service:\n    api_key: " + apiKey;
        try (
            QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:v1.7.4")
                .withConfigFile(Transferable.of(configFile))
        ) {
            qdrant.start();

            try (
                QdrantClient unauthClient = new QdrantClient(
                    QdrantGrpcClient.newBuilder(qdrant.getHost(), qdrant.getGrpcPort(), false).build()
                )
            ) {
                assertThatThrownBy(() -> unauthClient.healthCheckAsync().get()).isInstanceOf(ExecutionException.class);
            }

            try (
                QdrantClient client = new QdrantClient(
                    QdrantGrpcClient
                        .newBuilder(qdrant.getHost(), qdrant.getGrpcPort(), false)
                        .withApiKey(apiKey)
                        .build()
                )
            ) {
                QdrantOuterClass.HealthCheckReply healthCheckReply = client.healthCheckAsync().get();
                assertThat(healthCheckReply.getVersion()).isEqualTo("1.7.4");
            }
        }
    }
}
