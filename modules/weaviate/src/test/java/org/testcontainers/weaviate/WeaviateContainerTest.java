package org.testcontainers.weaviate;

import io.weaviate.client6.v1.api.InstanceMetadata;
import io.weaviate.client6.v1.api.WeaviateClient;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WeaviateContainerTest {

    @Test
    void testWeaviate() throws Exception {
        try ( // container {
            WeaviateContainer weaviate = new WeaviateContainer("cr.weaviate.io/semitechnologies/weaviate:1.32.0")
            // }
        ) {
            weaviate.start();
            try (
                WeaviateClient client = WeaviateClient.connectToCustom(conn -> {
                    return conn
                        .scheme("http")
                        .httpHost(weaviate.getHost())
                        .httpPort(weaviate.getMappedPort(8080))
                        .grpcHost(weaviate.getHost())
                        .grpcPort(weaviate.getMappedPort(50051));
                })
            ) {
                InstanceMetadata meta = client.meta();
                assertThat(meta.version()).isEqualTo("1.32.0");
            }
        }
    }

    @Test
    void testWeaviateWithModules() throws Exception {
        List<String> enableModules = Arrays.asList(
            "backup-filesystem",
            "text2vec-openai",
            "text2vec-cohere",
            "text2vec-huggingface",
            "generative-openai"
        );
        Map<String, String> env = new HashMap<>();
        env.put("ENABLE_MODULES", String.join(",", enableModules));
        env.put("BACKUP_FILESYSTEM_PATH", "/tmp/backups");
        try (WeaviateContainer weaviate = new WeaviateContainer("semitechnologies/weaviate:1.32.0").withEnv(env)) {
            weaviate.start();
            try (
                WeaviateClient client = WeaviateClient.connectToCustom(conn -> {
                    return conn
                        .scheme("http")
                        .httpHost(weaviate.getHost())
                        .httpPort(weaviate.getMappedPort(8080))
                        .grpcHost(weaviate.getHost())
                        .grpcPort(weaviate.getMappedPort(50051));
                })
            ) {
                InstanceMetadata meta = client.meta();
                assertThat(meta.version()).isEqualTo("1.32.0");
                Map<String, Object> modules = meta.modules();
                assertThat(modules)
                    .isNotNull()
                    .asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
                    .extracting(Map::keySet)
                    .satisfies(keys -> {
                        assertThat(keys.size()).isEqualTo(enableModules.size());
                        keys.forEach(key -> assertThat(enableModules.contains(key)).isTrue());
                    });
            }
        }
    }
}
