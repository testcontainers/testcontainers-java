package org.testcontainers.typesense;

import org.junit.Test;
import org.typesense.api.Client;
import org.typesense.api.Configuration;
import org.typesense.resources.Node;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TypesenseContainerTest {

    @Test
    public void query() throws Exception {
        try ( // container {
            TypesenseContainer typesense = new TypesenseContainer("typesense/typesense:27.1")
            // }
        ) {
            typesense.start();
            List<Node> nodes = Collections.singletonList(
                new Node("http", typesense.getHost(), typesense.getHttpPort())
            );

            assertThat(typesense.getApiKey()).isEqualTo("testcontainers");
            Configuration configuration = new Configuration(nodes, Duration.ofSeconds(5), typesense.getApiKey());
            Client client = new Client(configuration);
            System.out.println(client.health.retrieve());
            assertThat(client.health.retrieve()).containsEntry("ok", true);
        }
    }

    @Test
    public void withCustomApiKey() throws Exception {
        try (TypesenseContainer typesense = new TypesenseContainer("typesense/typesense:27.1").withApiKey("s3cr3t")) {
            typesense.start();
            List<Node> nodes = Collections.singletonList(
                new Node("http", typesense.getHost(), typesense.getHttpPort())
            );

            assertThat(typesense.getApiKey()).isEqualTo("s3cr3t");
            Configuration configuration = new Configuration(nodes, Duration.ofSeconds(5), typesense.getApiKey());
            Client client = new Client(configuration);
            assertThat(client.health.retrieve()).containsEntry("ok", true);
        }
    }
}
