package org.testcontainers.elasticsearch;

import com.github.dockerjava.api.DockerClient;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
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
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;

public class ElasticsearchContainerTest {

    /**
     * Elasticsearch version which should be used for the Tests
     */
    private static final String ELASTICSEARCH_VERSION = "7.9.2";
    private static final DockerImageName ELASTICSEARCH_IMAGE =
        DockerImageName
            .parse("docker.elastic.co/elasticsearch/elasticsearch")
            .withTag(ELASTICSEARCH_VERSION);

    /**
     * Elasticsearch default username, when secured
     */
    private static final String ELASTICSEARCH_USERNAME = "elastic";

    /**
     * From 6.8, we can optionally activate security with a default password.
     */
    private static final String ELASTICSEARCH_PASSWORD = "changeme";

    private RestClient client = null;
    private RestClient anonymousClient = null;

    @After
    public void stopRestClient() throws IOException {
        if (client != null) {
            client.close();
            client = null;
        }
        if (anonymousClient != null) {
            anonymousClient.close();
            anonymousClient = null;
        }
    }

    @SuppressWarnings("deprecation") // Using deprecated constructor for verification of backwards compatibility
    @Test
    @Deprecated // We will remove this test in the future
    public void elasticsearchDeprecatedCtorTest() throws IOException {
        // Create the elasticsearch container.
        try (ElasticsearchContainer container = new ElasticsearchContainer()
            .withEnv("foo", "bar") // dummy env for compiler checking correct generics usage
        ) {
            // Start the container. This step might take some time...
            container.start();

            // Do whatever you want with the rest client ...
            Response response = getClient(container).performRequest(new Request("GET", "/"));
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            assertThat(EntityUtils.toString(response.getEntity()), containsString(ELASTICSEARCH_VERSION));

            // The default image is running with the features under Elastic License
            response = getClient(container).performRequest(new Request("GET", "/_xpack/"));
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            // For now we test that we have the monitoring feature available
            assertThat(EntityUtils.toString(response.getEntity()), containsString("monitoring"));
        }
    }

    @Test
    public void elasticsearchDefaultTest() throws IOException {
        // Create the elasticsearch container.
        try (ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
            .withEnv("foo", "bar") // dummy env for compiler checking correct generics usage
        ) {
            // Start the container. This step might take some time...
            container.start();

            // Do whatever you want with the rest client ...
            Response response = getClient(container).performRequest(new Request("GET", "/"));
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            assertThat(EntityUtils.toString(response.getEntity()), containsString(ELASTICSEARCH_VERSION));

            // The default image is running with the features under Elastic License
            response = getClient(container).performRequest(new Request("GET", "/_xpack/"));
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            // For now we test that we have the monitoring feature available
            assertThat(EntityUtils.toString(response.getEntity()), containsString("monitoring"));
        }
    }

    @Test
    public void elasticsearchSecuredTest() throws IOException {
        try (ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
            .withPassword(ELASTICSEARCH_PASSWORD)) {
            container.start();

            // The cluster should be secured so it must fail when we try to access / without credentials
            assertThrows("We should not be able to access / URI with an anonymous client.",
                ResponseException.class,
                () -> getAnonymousClient(container).performRequest(new Request("GET", "/")));

            // But it should work when we try to access / with the proper login and password
            Response response = getClient(container).performRequest(new Request("GET", "/"));
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            assertThat(EntityUtils.toString(response.getEntity()), containsString(ELASTICSEARCH_VERSION));
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
                 // ossContainer {
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

    @Test
    public void restClientClusterHealth() throws IOException {
        // httpClientContainer {
        // Create the elasticsearch container.
        try (ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)) {
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
    public void restClientSecuredClusterHealth() throws IOException {
        // httpClientSecuredContainer {
        // Create the elasticsearch container.
        try (ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
            // With a password
            .withPassword(ELASTICSEARCH_PASSWORD)) {
            // Start the container. This step might take some time...
            container.start();

            // Create the secured client.
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
            // httpClientSecuredContainer {{
        }
        // }
    }

    @SuppressWarnings("deprecation") // The TransportClient will be removed in Elasticsearch 8.
    @Test
    public void transportClientClusterHealth() {
        // transportClientContainer {
        // Create the elasticsearch container.
        try (ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)){
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

    @Test
    public void incompatibleSettingsTest() {
        // The OSS image can not use security feature
        assertThrows("We should not be able to activate security with an OSS License",
            IllegalArgumentException.class,
            () -> new ElasticsearchContainer(
                DockerImageName
                    .parse("docker.elastic.co/elasticsearch/elasticsearch-oss")
                    .withTag(ELASTICSEARCH_VERSION))
            .withPassword("foo")
        );
    }

    @Test
    public void testElasticsearch8SecureByDefault() throws Exception {
        try (ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.1.2")) {
            // Start the container. This step might take some time...
            container.start();

            Response response = getClusterHealth(container);
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            assertThat(EntityUtils.toString(response.getEntity()), containsString("cluster_name"));
        }
    }

    @Test
    public void testElasticsearch8SecureByDefaultCustomCaCertFails() throws Exception {
        final MountableFile mountableFile = MountableFile.forClasspathResource("http_ca.crt");
        String caPath = "/tmp/http_ca.crt";
        try (ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.1.2")
            .withCopyToContainer(mountableFile, caPath)
            .withCertPath(caPath)) {

            container.start();

            // this is expected, as a different cert is used for creating the SSL context
            assertThrows("PKIX path validation failed: java.security.cert.CertPathValidatorException: Path does not chain with any of the trust anchors", SSLHandshakeException.class, () -> getClusterHealth(container));
        }
    }

    @Test
    public void testElasticsearch8SecureByDefaultHttpWaitStrategy() throws Exception {
        final HttpWaitStrategy httpsWaitStrategy = Wait.forHttps("/")
            .forPort(9200)
            .forStatusCode(200)
            .withBasicCredentials(ELASTICSEARCH_USERNAME, ELASTICSEARCH_PASSWORD)
            // trusting self-signed certificate
            .allowInsecure();

        try (ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.1.2")
            .waitingFor(httpsWaitStrategy)) {

            // Start the container. This step might take some time...
            container.start();

            Response response = getClusterHealth(container);
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            assertThat(EntityUtils.toString(response.getEntity()), containsString("cluster_name"));
        }
    }

    @Test
    public void testElasticsearch8SecureByDefaultFailsSilentlyOnLatestImages() throws Exception {
        // this test exists for custom images by users that use the `latest` tag
        // even though the version might be older than version 8
        // this tags an old 7.x version as :latest
        tagImage("docker.elastic.co/elasticsearch/elasticsearch:7.9.2", "elasticsearch-tc-older-release", "latest");
        DockerImageName image = DockerImageName.parse("elasticsearch-tc-older-release:latest").asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch");

        try (ElasticsearchContainer container = new ElasticsearchContainer(image)) {
            container.start();

            Response response = getClient(container).performRequest(new Request("GET", "/_cluster/health"));
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            assertThat(EntityUtils.toString(response.getEntity()), containsString("cluster_name"));
        }
    }

    private void tagImage(String sourceImage, String targetImage, String targetTag) throws InterruptedException {
        DockerClient dockerClient = DockerClientFactory.instance().client();
        dockerClient.tagImageCmd(
                new RemoteDockerImage(DockerImageName.parse(sourceImage)).get(),
                targetImage,
                targetTag
            )
            .exec();
    }

    private Response getClusterHealth(ElasticsearchContainer container) throws IOException {
        // Create the secured client.
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
            new UsernamePasswordCredentials(ELASTICSEARCH_USERNAME, ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD));

        client = RestClient.builder(HttpHost.create("https://" + container.getHttpHostAddress()))
            .setHttpClientConfigCallback(httpClientBuilder -> {
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                httpClientBuilder.setSSLContext(container.createSslContextFromCa());
                return httpClientBuilder;
            })
            .build();

        return client.performRequest(new Request("GET", "/_cluster/health"));
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

    private RestClient getAnonymousClient(ElasticsearchContainer container) {
        if (anonymousClient == null) {
            anonymousClient = RestClient.builder(HttpHost.create(container.getHttpHostAddress())).build();
        }

        return anonymousClient;
    }
}
