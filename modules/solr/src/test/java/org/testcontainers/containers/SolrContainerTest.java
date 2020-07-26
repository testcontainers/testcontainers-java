package org.testcontainers.containers;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.junit.After;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Simon Schneider
 */
public class SolrContainerTest {

    private static final DockerImageName SOLR_IMAGE = DockerImageName.parse("solr:8.3.0");
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
        try (SolrContainer container = new SolrContainer(SOLR_IMAGE)) {
            container.start();
            SolrPingResponse response = getClient(container).ping("dummy");
            assertThat(response.getStatus(), is(0));
            assertThat(response.jsonStr(), containsString("zkConnected\":true"));
        }
    }

    @Test
    public void solrStandaloneTest() throws IOException, SolrServerException {
        try (SolrContainer container = new SolrContainer(SOLR_IMAGE).withZookeeper(false)) {
            container.start();
            SolrPingResponse response = getClient(container).ping("dummy");
            assertThat(response.getStatus(), is(0));
            assertThat(response.jsonStr(), containsString("zkConnected\":null"));
        }
    }

    @Test
    public void solrCloudPingTest() throws IOException, SolrServerException {
        // solrContainerUsage {
        // Create the solr container.
        SolrContainer container = new SolrContainer(SOLR_IMAGE);

        // Start the container. This step might take some time...
        container.start();

        // Do whatever you want with the client ...
        SolrClient client = new Http2SolrClient.Builder("http://" + container.getContainerIpAddress() + ":" + container.getSolrPort() + "/solr").build();
        SolrPingResponse response = client.ping("dummy");

        // Stop the container.
        container.stop();
        // }
    }

    private SolrClient getClient(SolrContainer container) {
        if (client == null) {
            client = new Http2SolrClient.Builder("http://" + container.getContainerIpAddress() + ":" + container.getSolrPort() + "/solr").build();
        }
        return client;
    }

}
