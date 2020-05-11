package org.testcontainers.containers;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.junit.After;
import org.junit.Test;

/**
 * @author Simon Schneider
 */
public class SolrContainerTest {

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
        try (SolrContainer container = new SolrContainer()) {
            container.start();
            SolrPingResponse response = getClient(container).ping("dummy");
            assertThat(response.getStatus(), is(0));
            assertThat(response.jsonStr(), containsString("zkConnected\":true"));
        }
    }

    @Test
    public void solrStandaloneTest() throws IOException, SolrServerException {
        try (SolrContainer container = new SolrContainer().withZookeeper(false)) {
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
        SolrContainer container = new SolrContainer();

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
