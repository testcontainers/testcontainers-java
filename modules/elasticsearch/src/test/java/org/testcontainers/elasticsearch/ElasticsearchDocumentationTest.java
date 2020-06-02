package org.testcontainers.elasticsearch;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.Test;

public class ElasticsearchDocumentationTest {

    @Test
    public void containerWithHttpClient() throws IOException {
        // httpClientContainerStart {
        // Create the elasticsearch container.
        ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:6.4.1");

        // Start the container. This step might take some time...
        container.start();

        // Do whatever you want with the rest client ...
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "changeme"));
        RestClient restClient = RestClient.builder(HttpHost.create(container.getHttpHostAddress()))
            .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
            .build();
        Response response = restClient.performRequest(new Request("GET", "/"));
        // }
        // Compare Result
        assertEquals(response.getStatusLine().getStatusCode(), 200);

        // httpClientContainerStop {
        // Stop the container.
        container.stop();
        // }
    }

    @Test
    public void containerWithTransportClient() {
        // Create the elasticsearch container.
        ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:6.4.1");

        // Start the container. This step might take some time...
        container.start();

        // transportClientContainerStart {
        // ... or the transport client
        TransportAddress transportAddress = new TransportAddress(container.getTcpHost());
        Settings settings = Settings.builder().put("cluster.name", "docker-cluster").build();
        TransportClient transportClient = new PreBuiltTransportClient(settings)
            .addTransportAddress(transportAddress);
        ClusterHealthResponse healths = transportClient.admin().cluster().prepareHealth().get();
        // }
        // Compare Result
        assertEquals(ClusterHealthStatus.GREEN, healths.getStatus());

        // Stop the container.
        container.stop();
    }

    @Test
    public void containerWithOpenSourceImage() {
        // oosContainer {
        // Create the elasticsearch container.
        ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch-oss:6.4.1");
        // }

        // Start the container. This step might take some time...
        container.start();

        // Stop the container.
        container.stop();
    }

}
