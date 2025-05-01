package org.testcontainers.weaviate;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.misc.model.Meta;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class WeaviateContainerTest {

    @Test
    public void testWeaviate() {
        try ( // container {
            WeaviateContainer weaviate = new WeaviateContainer("cr.weaviate.io/semitechnologies/weaviate:1.29.0")
            // }
        ) {
            weaviate.start();
            Config config = new Config("http", weaviate.getHttpHostAddress());
            config.setGRPCHost(weaviate.getGrpcHostAddress());
            WeaviateClient client = new WeaviateClient(config);
            Result<Meta> meta = client.misc().metaGetter().run();
            assertThat(meta.getResult().getVersion()).isEqualTo("1.29.0");
        }
    }

    @Test
    public void testWeaviateWithModules() {
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
        try (WeaviateContainer weaviate = new WeaviateContainer("semitechnologies/weaviate:1.29.0").withEnv(env)) {
            weaviate.start();
            Config config = new Config("http", weaviate.getHttpHostAddress());
            config.setGRPCHost(weaviate.getGrpcHostAddress());
            WeaviateClient client = new WeaviateClient(config);
            Result<Meta> meta = client.misc().metaGetter().run();
            assertThat(meta.getResult().getVersion()).isEqualTo("1.29.0");
            Object modules = meta.getResult().getModules();
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
