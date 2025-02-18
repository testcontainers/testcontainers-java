package org.testcontainers.containers;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class SolrContainerTest {

    @Parameterized.Parameters(name = "{0}")
    public static String[] getVersionsToTest() {
        return new String[] { "solr:8.11.4", "solr:9.8.0" };
    }

    @Parameterized.Parameter
    public String solrImage;

    private SolrClient client = null;

    @After
    public void stopRestClient() throws IOException {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Test
    public void solrCloudTest() throws IOException, SolrServerException {
        try (SolrContainer container = new SolrContainer(solrImage)) {
            container.start();
            SolrPingResponse response = getClient(container).ping("dummy");
            assertThat(response.getStatus()).isZero();
            assertThat(response.jsonStr()).contains("zkConnected\":true");
        }
    }

    @Test
    public void solrStandaloneTest() throws IOException, SolrServerException {
        try (SolrContainer container = new SolrContainer(solrImage).withZookeeper(false)) {
            container.start();
            SolrPingResponse response = getClient(container).ping("dummy");
            assertThat(response.getStatus()).isZero();
            assertThat(response.jsonStr()).contains("zkConnected\":null");
        }
    }

    @Test
    public void solrCloudPingTest() throws IOException, SolrServerException {
        // solrContainerUsage {
        // Create the solr container.
        SolrContainer container = new SolrContainer(solrImage);

        // Start the container. This step might take some time...
        container.start();

        // Do whatever you want with the client ...
        SolrClient client = new Http2SolrClient.Builder(
            "http://" + container.getHost() + ":" + container.getSolrPort() + "/solr"
        )
            .build();
        SolrPingResponse response = client.ping("dummy");

        // Stop the container.
        container.stop();
        // }
    }

    private SolrClient getClient(SolrContainer container) {
        if (client == null) {
            client =
                new Http2SolrClient.Builder("http://" + container.getHost() + ":" + container.getSolrPort() + "/solr")
                    .build();
        }
        return client;
    }
}
