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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;

import javax.net.ssl.SSLHandshakeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ElasticsearchContainerTest {

    /**
     * Elasticsearch 7.x version to use in tests.
     */
    // version_7 {
    static final String ELASTICSEARCH_VERSION_7 = "7.17.29";
    static final DockerImageName ELASTICSEARCH_IMAGE_7 = DockerImageName.parse(
        "docker.elastic.co/elasticsearch/elasticsearch")
        .withTag(ELASTICSEARCH_VERSION_7);
    // }

    /**
     * Elasticsearch 8.x version to use in tests.
     */
    // version_8 {
    static final String ELASTICSEARCH_VERSION_8 = "8.19.20";
    static final DockerImageName ELASTICSEARCH_IMAGE_8 = DockerImageName.parse(
        "docker.elastic.co/elasticsearch/elasticsearch")
        .withTag(ELASTICSEARCH_VERSION_8);
    // }

    /**
     * Elasticsearch 9.x version to use in tests.
     */
    // version_9 {
    static final String ELASTICSEARCH_VERSION_9 = "9.5.2";
    static final DockerImageName ELASTICSEARCH_IMAGE_9 = DockerImageName.parse(
        "docker.elastic.co/elasticsearch/elasticsearch")
        .withTag(ELASTICSEARCH_VERSION_9);
    // }

    /**
     * Latest Elasticsearch version exercised by default in these tests.
     * Point this at the next major when adding coverage for it.
     */
    static final String ELASTICSEARCH_VERSION_LATEST = ELASTICSEARCH_VERSION_9;

    static final DockerImageName ELASTICSEARCH_IMAGE_LATEST = ELASTICSEARCH_IMAGE_9;

    /**
     * Elasticsearch default username, when secured
     */
    private static final String ELASTICSEARCH_USERNAME = "elastic";

    /**
     * Default password used by ElasticsearchContainer for versions &gt;= 8, and by withPassword() in these tests.
     */
    private static final String ELASTICSEARCH_PASSWORD = "changeme";

    private RestClient client = null;

    private RestClient anonymousClient = null;

    @AfterEach
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

    // Latest (default coverage)

    @Test
    void latestStartsWithTlsAndPassword() throws IOException {
        // httpClientLatest {
        // Create the elasticsearch container.
        try (ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST)) {
            // Start the container. This step might take some time...
            container.start();

            // Do whatever you want with the rest client ...
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(ELASTICSEARCH_USERNAME, ELASTICSEARCH_PASSWORD)
            );

            client =
                RestClient
                    .builder(HttpHost.create("https://" + container.getHttpHostAddress()))
                    .setHttpClientConfigCallback(httpClientBuilder -> {
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                        // SSL is activated by default in Elasticsearch 8+
                        httpClientBuilder.setSSLContext(container.createSslContextFromCa());
                        return httpClientBuilder;
                    })
                    .build();

            Response response = client.performRequest(new Request("GET", "/_cluster/health"));
            // }}
            assertThat(response.getStatusLine().getStatusCode()).as("cluster health is available").isEqualTo(200);
            assertThat(EntityUtils.toString(response.getEntity())).contains("cluster_name");
            assertThat(container.getHttpScheme()).as("HTTP API uses HTTPS by default").isEqualTo("https");

            response = client.performRequest(new Request("GET", "/"));
            assertThat(response.getStatusLine().getStatusCode()).as("root endpoint is available").isEqualTo(200);
            assertThat(EntityUtils.toString(response.getEntity()))
                .as("reported version matches the latest image")
                .contains(ELASTICSEARCH_VERSION_LATEST);

            response = client.performRequest(new Request("GET", "/_xpack/"));
            assertThat(response.getStatusLine().getStatusCode()).as("xpack API is available").isEqualTo(200);
            assertThat(EntityUtils.toString(response.getEntity()))
                .as("Elastic licensed features are available")
                .contains("monitoring");

            assertThat(catchThrowable(() -> getAnonymousClient(container).performRequest(new Request("GET", "/"))))
                .as("anonymous requests are rejected")
                .isInstanceOf(ResponseException.class);
            // httpClientLatest {{
        }
        // }
    }

    @Test
    void latestCanDisableTls() throws IOException {
        // httpClientTlsDisabled {
        // Create the elasticsearch container.
        try (
            ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST)
                // disable SSL
                .withEnv("xpack.security.transport.ssl.enabled", "false")
                .withEnv("xpack.security.http.ssl.enabled", "false")
        ) {
            // Start the container. This step might take some time...
            container.start();

            // Do whatever you want with the rest client ...
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(ELASTICSEARCH_USERNAME, ELASTICSEARCH_PASSWORD)
            );

            client =
                RestClient
                    .builder(HttpHost.create(container.getHttpHostAddress()))
                    .setHttpClientConfigCallback(httpClientBuilder -> {
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    })
                    .build();

            Response response = client.performRequest(new Request("GET", "/_cluster/health"));
            // }}
            assertThat(response.getStatusLine().getStatusCode()).as("cluster health is available").isEqualTo(200);
            assertThat(EntityUtils.toString(response.getEntity())).contains("cluster_name");
            assertThat(container.getHttpScheme()).as("HTTP API uses HTTP when TLS is disabled").isEqualTo("http");
            // httpClientTlsDisabled {{
        }
        // }
    }

    @Test
    void latestRejectsMismatchedCa() throws Exception {
        final MountableFile mountableFile = MountableFile.forClasspathResource("http_ca.crt");
        String caPath = "/tmp/http_ca.crt";
        try (
            ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST)
                .withCopyToContainer(mountableFile, caPath)
                .withCertPath(caPath)
        ) {
            container.start();

            assertThat(catchThrowable(() -> getClusterHealth(container)))
                .as("TLS handshake fails when the configured CA does not match the cluster")
                .isInstanceOf(SSLHandshakeException.class);
        }
    }

    @Test
    void latestHonorsCustomHttpsWaitStrategy() throws Exception {
        final HttpWaitStrategy httpsWaitStrategy = Wait
            .forHttps("/")
            .forPort(9200)
            .forStatusCode(200)
            .withBasicCredentials(ELASTICSEARCH_USERNAME, ELASTICSEARCH_PASSWORD)
            // trusting self-signed certificate
            .allowInsecure();

        try (
            ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST)
                .waitingFor(httpsWaitStrategy)
        ) {
            container.start();

            assertClusterHealthResponse(container);
        }
    }

    @Test
    void latestWorksWithDockerHubImage() throws Exception {
        try (
            ElasticsearchContainer container = new ElasticsearchContainer(
                "elasticsearch:" + ELASTICSEARCH_VERSION_LATEST
            )
        ) {
            container.start();

            assertClusterHealthResponse(container);
        }
    }

    @Test
    void latestDefaultHeapIsTwoGb() throws Exception {
        // Default is 2g, see https://www.elastic.co/guide/en/elasticsearch/reference/current/heap-size.html
        long defaultHeapSize = 2_147_483_648L;

        try (ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST)) {
            container.start();
            assertElasticsearchContainerHasHeapSize(container, defaultHeapSize);
        }
    }

    @Test
    void latestHeapCanBeSetViaEnv() throws Exception {
        String customHeapSize = "1500m";
        try (
            ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST)
                .withEnv("ES_JAVA_OPTS", String.format("-Xms%s  -Xmx%s", customHeapSize, customHeapSize))
        ) {
            container.start();
            assertElasticsearchContainerHasHeapSize(container, 1_572_864_000L);
        }
    }

    @Test
    void latestHeapCanBeSetViaJvmOptionsFile() throws Exception {
        try (
            ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST)
                .withClasspathResourceMapping(
                    "test-custom-memory-jvm.options",
                    "/usr/share/elasticsearch/config/jvm.options.d/a-user-defined-jvm.options",
                    BindMode.READ_ONLY
                );
        ) {
            container.start();
            assertElasticsearchContainerHasHeapSize(container, 1_572_864_000L);
        }
    }

    // Elasticsearch 8 (maintained)

    @Test
    void v8StartsWithDefaults() throws IOException {
        try (ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_8)) {
            container.start();
            Response response = getClient(container).performRequest(new Request("GET", "/"));
            assertThat(response.getStatusLine().getStatusCode()).as("root endpoint is available").isEqualTo(200);
            assertThat(EntityUtils.toString(response.getEntity()))
                .as("reported version matches the 8.x image")
                .contains(ELASTICSEARCH_VERSION_8);
            assertThat(container.getHttpScheme()).as("HTTP API uses HTTPS by default").isEqualTo("https");
        }
    }

    // Elasticsearch 7 (deprecated)

    @Test
    void v7UsesHttpWithoutSecurityByDefault() throws IOException {
        // httpClientV7 {
        // Create the elasticsearch container.
        try (ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_7)) {
            // Start the container. This step might take some time...
            container.start();

            client = RestClient.builder(HttpHost.create(container.getHttpHostAddress())).build();

            Response response = client.performRequest(new Request("GET", "/_cluster/health"));
            // }}
            assertThat(response.getStatusLine().getStatusCode()).as("cluster health is available").isEqualTo(200);
            assertThat(EntityUtils.toString(response.getEntity())).contains("cluster_name");
            assertThat(container.getHttpScheme()).as("HTTP API uses HTTP by default on 7.x").isEqualTo("http");
            // httpClientV7 {{
        }
        // }
    }

    @Test
    void v7EnablesSecurityWithPassword() throws IOException {
        // httpClientV7Secured {
        // Create the elasticsearch container.
        try (
            ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_7)
                // With a password
                .withPassword(ELASTICSEARCH_PASSWORD)
        ) {
            // Start the container. This step might take some time...
            container.start();

            // Create the secured client.
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(ELASTICSEARCH_USERNAME, ELASTICSEARCH_PASSWORD)
            );

            client =
                RestClient
                    .builder(HttpHost.create(container.getHttpHostAddress()))
                    .setHttpClientConfigCallback(httpClientBuilder -> {
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    })
                    .build();

            Response response = client.performRequest(new Request("GET", "/_cluster/health"));
            // }}
            assertThat(response.getStatusLine().getStatusCode()).as("cluster health is available").isEqualTo(200);
            assertThat(EntityUtils.toString(response.getEntity())).contains("cluster_name");
            // httpClientV7Secured {{
        }
        // }
    }

    @Test
    void v7SupportsCustomTlsCertificates() throws Exception {
        String customizedCertPath = "/usr/share/elasticsearch/config/certs/http_ca_customized.crt";
        try (
            ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_7)
                .withPassword(ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD)
                .withEnv("xpack.security.enabled", "true")
                .withEnv("xpack.security.http.ssl.enabled", "true")
                .withEnv("xpack.security.http.ssl.key", "/usr/share/elasticsearch/config/certs/elasticsearch.key")
                .withEnv(
                    "xpack.security.http.ssl.certificate",
                    "/usr/share/elasticsearch/config/certs/elasticsearch.crt"
                )
                .withEnv("xpack.security.http.ssl.certificate_authorities", customizedCertPath)
                // these lines show how certificates can be created self-made way
                // obviously this shouldn't be done in prod environment, where proper and officially signed keys should be present
                .withCopyToContainer(
                    Transferable.of(
                        "#!/bin/bash\n" +
                        "mkdir -p /usr/share/elasticsearch/config/certs;" +
                        "openssl req -x509 -newkey rsa:4096 -keyout /usr/share/elasticsearch/config/certs/elasticsearch.key -out /usr/share/elasticsearch/config/certs/elasticsearch.crt -days 365 -nodes -subj \"/CN=localhost\";" +
                        "openssl x509 -outform der -in /usr/share/elasticsearch/config/certs/elasticsearch.crt -out " +
                        customizedCertPath +
                        "; chown -R elasticsearch /usr/share/elasticsearch/config/certs/",
                        555
                    ),
                    "/usr/share/elasticsearch/generate-certs.sh"
                )
                // because we need to generate the certificates before Elasticsearch starts, the entry command has to be tuned accordingly
                .withCommand(
                    "sh",
                    "-c",
                    "/usr/share/elasticsearch/generate-certs.sh && /usr/local/bin/docker-entrypoint.sh"
                )
                .withCertPath(customizedCertPath)
        ) {
            container.start();
            assertClusterHealthResponse(container);
        }
    }

    @SuppressWarnings("deprecation") // The TransportClient will be removed in Elasticsearch 8.
    @Test
    void v7TransportClientCanQueryClusterHealth() {
        // transportClientV7 {
        // Create the elasticsearch container.
        try (ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_7)) {
            // Start the container. This step might take some time...
            container.start();

            // Do whatever you want with the transport client
            TransportAddress transportAddress = new TransportAddress(container.getTcpHost());
            String expectedClusterName = "docker-cluster";
            Settings settings = Settings.builder().put("cluster.name", expectedClusterName).build();
            try (
                TransportClient transportClient = new PreBuiltTransportClient(settings)
                    .addTransportAddress(transportAddress)
            ) {
                ClusterHealthResponse healths = transportClient.admin().cluster().prepareHealth().get();
                String clusterName = healths.getClusterName();
                // }}}
                assertThat(clusterName)
                    .as("TransportClient sees the default docker-cluster name")
                    .isEqualTo(expectedClusterName);
                // transportClientV7 {{{
            }
        }
        // }
    }

    // OSS 7.10.2

    @Test
    void ossImageHasNoXpackEndpoint() throws IOException {
        try (
            // ossContainer {
            ElasticsearchContainer container = new ElasticsearchContainer(
                "docker.elastic.co/elasticsearch/elasticsearch-oss:7.10.2"
            )
            // }
        ) {
            container.start();
            Response response = getClient(container).performRequest(new Request("GET", "/"));
            assertThat(response.getStatusLine().getStatusCode()).as("OSS image starts").isEqualTo(200);
            // The OSS image does not have any feature under Elastic License
            assertThat(catchThrowable(() -> getClient(container).performRequest(new Request("GET", "/_xpack/"))))
                .as("OSS image does not expose the /_xpack endpoint")
                .isInstanceOf(ResponseException.class);
        }
    }

    @Test
    void ossImageRejectsPassword() {
        // The OSS image can not use security feature
        assertThat(
            catchThrowable(() -> {
                new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch-oss:7.10.2")
                    .withPassword("foo");
            })
        )
            .as("password cannot be set on an OSS image")
            .isInstanceOf(IllegalArgumentException.class);
    }

    // Non-semantic image tags

    @Test
    void nonSemanticLatestTagStillStarts() throws Exception {
        // Users sometimes tag custom or older images as :latest. The version part is then not
        // a semantic version, but ComparableVersion still treats it as >= 8.0.0, so the
        // container applies the 8+ defaults. Starting a 7.x image under that tag must still work
        // (the CA cert is missing and is ignored).
        tagImage(ELASTICSEARCH_IMAGE_7.asCanonicalNameString(), "elasticsearch-tc-older-release", "latest");
        DockerImageName image = DockerImageName
            .parse("elasticsearch-tc-older-release:latest")
            .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch");

        try (ElasticsearchContainer container = new ElasticsearchContainer(image)) {
            container.start();

            Response response = getClient(container).performRequest(new Request("GET", "/_cluster/health"));
            assertThat(response.getStatusLine().getStatusCode())
                .as("cluster health is available with a non-semantic :latest tag")
                .isEqualTo(200);
            assertThat(EntityUtils.toString(response.getEntity())).contains("cluster_name");
        }
    }

    private void tagImage(String sourceImage, String targetImage, String targetTag) throws InterruptedException {
        DockerClient dockerClient = DockerClientFactory.instance().client();
        dockerClient
            .tagImageCmd(new RemoteDockerImage(DockerImageName.parse(sourceImage)).get(), targetImage, targetTag)
            .exec();
    }

    private Response getClusterHealth(ElasticsearchContainer container) throws IOException {
        // Create the secured client.
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
            AuthScope.ANY,
            new UsernamePasswordCredentials(
                ELASTICSEARCH_USERNAME,
                ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD
            )
        );

        client =
            RestClient
                .builder(HttpHost.create("https://" + container.getHttpHostAddress()))
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
            credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(ELASTICSEARCH_USERNAME, ELASTICSEARCH_PASSWORD)
            );

            String protocol = container.caCertAsBytes().isPresent() ? "https://" : "http://";

            client =
                RestClient
                    .builder(HttpHost.create(protocol + container.getHttpHostAddress()))
                    .setHttpClientConfigCallback(httpClientBuilder -> {
                        if (container.caCertAsBytes().isPresent()) {
                            httpClientBuilder.setSSLContext(container.createSslContextFromCa());
                        }
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    })
                    .build();
        }

        return client;
    }

    private RestClient getAnonymousClient(ElasticsearchContainer container) {
        if (anonymousClient == null) {
            String protocol = container.caCertAsBytes().isPresent() ? "https://" : "http://";
            anonymousClient =
                RestClient
                    .builder(HttpHost.create(protocol + container.getHttpHostAddress()))
                    .setHttpClientConfigCallback(httpClientBuilder -> {
                        if (container.caCertAsBytes().isPresent()) {
                            httpClientBuilder.setSSLContext(container.createSslContextFromCa());
                        }
                        return httpClientBuilder;
                    })
                    .build();
        }

        return anonymousClient;
    }

    private void assertElasticsearchContainerHasHeapSize(ElasticsearchContainer container, long heapSizeInBytes)
        throws Exception {
        Response response = getClient(container).performRequest(new Request("GET", "/_nodes/_all/jvm"));
        String responseBody = EntityUtils.toString(response.getEntity());
        assertThat(response.getStatusLine().getStatusCode()).as("nodes JVM stats are available").isEqualTo(200);
        assertThat(responseBody)
            .as("initial heap size matches the configured value")
            .contains("\"heap_init_in_bytes\":" + heapSizeInBytes);
        assertThat(responseBody)
            .as("maximum heap size matches the configured value")
            .contains("\"heap_max_in_bytes\":" + heapSizeInBytes);
    }

    private void assertClusterHealthResponse(ElasticsearchContainer container) throws IOException {
        Response response = getClusterHealth(container);
        assertThat(response.getStatusLine().getStatusCode()).as("cluster health is available").isEqualTo(200);
        assertThat(EntityUtils.toString(response.getEntity())).contains("cluster_name");
    }
}
