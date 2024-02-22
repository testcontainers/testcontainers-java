package org.testcontainers.qdrant;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.QdrantOuterClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class QdrantContainerTest {

    @Test
    public void test() throws ExecutionException, InterruptedException {
        try (
            // qdrantContainer {
            QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:v1.7.4")
            // }
        ) {
            qdrant.start();

            QdrantGrpcClient client = QdrantGrpcClient
                .newBuilder(
                    Grpc.newChannelBuilder(qdrant.getGrpcHostAddress(), InsecureChannelCredentials.create()).build()
                )
                .build();
            QdrantOuterClass.HealthCheckReply healthCheckReply = client
                .qdrant()
                .healthCheck(QdrantOuterClass.HealthCheckRequest.getDefaultInstance())
                .get();
            assertThat(healthCheckReply.getVersion()).isEqualTo("1.7.4");
        }
    }
}
