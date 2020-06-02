package org.testcontainers.elasticsearch;

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
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.Test;

public class ElasticsearchDocumentationTest {

    @Test
    public void containerWithHttpClient() throws IOException {
        // httpClientContainer {
        // Create the elasticsearch container.
        try (ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:6.4.1")) {
            // Start the container. This step might take some time...
            container.start();

            // Do whatever you want with the rest client ...
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "changeme"));
            RestClient restClient = RestClient.builder(HttpHost.create(container.getHttpHostAddress()))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                .build();
            Response response = restClient.performRequest(new Request("GET", "/"));

        }
        // }
    }

    @Test
    public void containerWithTransportClient() {
        // transportClientContainer {
        // Create the elasticsearch container.
        try (ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:6.4.1")) {
            // Start the container. This step might take some time...
            container.start();

            // Do whatever you want with the transport client
            TransportAddress transportAddress = new TransportAddress(container.getTcpHost());
            Settings settings = Settings.builder().put("cluster.name", "docker-cluster").build();
            TransportClient transportClient = new PreBuiltTransportClient(settings)
                .addTransportAddress(transportAddress);
            ClusterHealthResponse healths = transportClient.admin().cluster().prepareHealth().get();

        }
        // }
    }

    @Test
    public void containerWithOpenSourceImage() {
        // oosContainer {
        // Create the elasticsearch container.
        ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch-oss:6.4.1");
        // }
    }

}
