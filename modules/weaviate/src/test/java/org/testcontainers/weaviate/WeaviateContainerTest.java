package org.testcontainers.weaviate;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.misc.model.Meta;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WeaviateContainerTest {

    @Test
    public void test() {
        try ( // container {
            WeaviateContainer weaviate = new WeaviateContainer("semitechnologies/weaviate:1.22.4")
            // }
        ) {
            weaviate.start();
            WeaviateClient client = new WeaviateClient(new Config("http", weaviate.getEndpoint()));
            Result<Meta> meta = client.misc().metaGetter().run();
            assertThat(meta.getResult().getVersion()).isEqualTo("1.22.4");
        }
    }
}
