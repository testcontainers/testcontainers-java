package org.testcontainers.pinecone;

import io.pinecone.clients.Pinecone;
import org.junit.Test;
import org.openapitools.db_control.client.model.DeletionProtection;
import org.openapitools.db_control.client.model.IndexModel;

import static org.assertj.core.api.Assertions.assertThat;

public class PineconeLocalContainerTest {

    @Test
    public void testSimple() {
        try ( // container {
            PineconeLocalContainer container = new PineconeLocalContainer("ghcr.io/pinecone-io/pinecone-local:v0.7.0")
            // }
        ) {
            container.start();

            // client {
            Pinecone pinecone = new Pinecone.Builder("pclocal")
                .withHost(container.getEndpoint())
                .withTlsEnabled(false)
                .build();
            // }

            String indexName = "example-index";
            pinecone.createServerlessIndex(indexName, "cosine", 2, "aws", "us-east-1", DeletionProtection.DISABLED);
            IndexModel indexModel = pinecone.describeIndex(indexName);
            assertThat(indexModel.getDeletionProtection()).isEqualTo(DeletionProtection.DISABLED);
        }
    }
}
