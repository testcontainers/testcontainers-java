package org.testcontainers.containers;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class SolrContainerTest {

    private static String[] getVersionsToTest() {
        return new String[] { "solr:8.11.4", "solr:9.8.0" };
    }

    @ParameterizedTest
    @MethodSource("getVersionsToTest")
    void solrCloudTest(String solrImage) throws IOException, SolrServerException {
        try (SolrContainer container = new SolrContainer(solrImage)) {
            container.start();
            SolrPingResponse response = getResponse(container);
            assertThat(response.getStatus()).isZero();
            assertThat(response.jsonStr()).contains("zkConnected\":true");
        }
    }

    @ParameterizedTest
    @MethodSource("getVersionsToTest")
    void solrStandaloneTest(String solrImage) throws IOException, SolrServerException {
        try (SolrContainer container = new SolrContainer(solrImage).withZookeeper(false)) {
            container.start();
            SolrPingResponse response = getResponse(container);
            assertThat(response.getStatus()).isZero();
            assertThat(response.jsonStr()).contains("zkConnected\":null");
        }
    }

    private SolrPingResponse getResponse(SolrContainer container) throws SolrServerException, IOException {
        try (
            SolrClient client = new Http2SolrClient.Builder(
                "http://" + container.getHost() + ":" + container.getSolrPort() + "/solr"
            )
                .build()
        ) {
            return client.ping("dummy");
        }
    }
}
