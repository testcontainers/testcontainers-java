package org.testcontainers.elasticsearch;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.After;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;
import static org.testcontainers.elasticsearch.ElasticsearchContainer.ELASTICSEARCH_DEFAULT_VERSION;

public class ElasticsearchContainerTest {

    /**
     * Elasticsearch version which should be used for the Tests
     */
    private static final String ELASTICSEARCH_VERSION = Version.CURRENT.toString();
    private static final DockerImageName ELASTICSEARCH_IMAGE =
        DockerImageName
            .parse("docker.elastic.co/elasticsearch/elasticsearch")
            .withTag(ELASTICSEARCH_VERSION);

    /**
     * Elasticsearch default username, when secured with a license > basic
     */
    private static final String ELASTICSEARCH_USERNAME = "elastic";

    /**
     * Elasticsearch 5.x default password. In 6.x images, there's no security by default as shipped with a basic license.
     */
    private static final String ELASTICSEARCH_PASSWORD = "changeme";

    private RestClient client = null;

    @After
    public void stopRestClient() throws IOException {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @SuppressWarnings("deprecation") // Using deprecated constructor for verification of backwards compatibility
    @Test
    public void elasticsearchDefaultTest() throws IOException {
        // Create the elasticsearch container.
        try (ElasticsearchContainer container = new ElasticsearchContainer()
            .withEnv("foo", "bar") // dummy env for compiler checking correct generics usage
        ) {
            // Start the container. This step might take some time...
            container.start();

            // Do whatever you want with the rest client ...
            Response response = getClient(container).performRequest(new Request("GET", "/"));
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            assertThat(EntityUtils.toString(response.getEntity()), containsString(ELASTICSEARCH_DEFAULT_VERSION));

            // The default image is running with the features under Elastic License
            response = getClient(container).performRequest(new Request("GET", "/_xpack/"));
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            // For now we test that we have the monitoring feature available
            assertThat(EntityUtils.toString(response.getEntity()), containsString("monitoring"));
        }
    }

    @Test
    public void elasticsearchVersion() throws IOException {
        try (ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)) {
            container.start();
            Response response = getClient(container).performRequest(new Request("GET", "/"));
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            String responseAsString = EntityUtils.toString(response.getEntity());
            assertThat(responseAsString, containsString(ELASTICSEARCH_VERSION));
        }
    }

    @Test
    public void elasticsearchOssImage() throws IOException {
        try (ElasticsearchContainer container =
                 // oosContainer {
                 new ElasticsearchContainer(
                     DockerImageName
                         .parse("docker.elastic.co/elasticsearch/elasticsearch-oss")
                         .withTag(ELASTICSEARCH_VERSION)
                 )
             // }
        ) {
            container.start();
            Response response = getClient(container).performRequest(new Request("GET", "/"));
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            // The OSS image does not have any feature under Elastic License
            assertThrows("We should not have /_xpack endpoint with an OSS License",
                ResponseException.class,
                () -> getClient(container).performRequest(new Request("GET", "/_xpack/")));
        }
    }

    @SuppressWarnings("deprecation") // Using deprecated constructor for verification of backwards compatibility
    @Test
    public void restClientClusterHealth() throws IOException {
        // httpClientContainer {
        // Create the elasticsearch container.
        try (ElasticsearchContainer container = new ElasticsearchContainer()) {
            // Start the container. This step might take some time...
            container.start();

            // Do whatever you want with the rest client ...
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(ELASTICSEARCH_USERNAME, ELASTICSEARCH_PASSWORD));

            client = RestClient.builder(HttpHost.create(container.getHttpHostAddress()))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                .build();

            Response response = client.performRequest(new Request("GET", "/_cluster/health"));
            // }}
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            assertThat(EntityUtils.toString(response.getEntity()), containsString("cluster_name"));
            // httpClientContainer {{
        }
        // }
    }

    @Test
    public void transportClientClusterHealth() {
        // transportClientContainer {
        // Create the elasticsearch container.
        try (ElasticsearchContainer container = new ElasticsearchContainer(
            DockerImageName
                .parse("docker.elastic.co/elasticsearch/elasticsearch")
                .withTag("6.4.1")
        )){
            // Start the container. This step might take some time...
            container.start();

            // Do whatever you want with the transport client
            TransportAddress transportAddress = new TransportAddress(container.getTcpHost());
            String expectedClusterName = "docker-cluster";
            Settings settings = Settings.builder().put("cluster.name", expectedClusterName).build();
            try (TransportClient transportClient = new PreBuiltTransportClient(settings)
                .addTransportAddress(transportAddress)) {
                ClusterHealthResponse healths = transportClient.admin().cluster().prepareHealth().get();
                String clusterName = healths.getClusterName();
                // }}}
                assertThat(clusterName, is(expectedClusterName));
                // transportClientContainer {{{
            }
        }
        // }
    }

    private RestClient getClient(ElasticsearchContainer container) {
        if (client == null) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(ELASTICSEARCH_USERNAME, ELASTICSEARCH_PASSWORD));

            client = RestClient.builder(HttpHost.create(container.getHttpHostAddress()))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                .build();
        }

        return client;
    }

}
