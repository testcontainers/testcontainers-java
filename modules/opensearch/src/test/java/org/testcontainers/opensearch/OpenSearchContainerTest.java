package org.testcontainers.opensearch;

import com.github.dockerjava.api.DockerClient;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Test;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;

public class OpenSearchContainerTest {

    /**
     * OpenSearch version which should be used for the Tests
     */
    private static final String OPENSEARCH_VERSION = "2.1.0";

    private static final DockerImageName OPENSEARCH_IMAGE = DockerImageName
        .parse("opensearchproject/opensearch")
        .withTag(OPENSEARCH_VERSION);

    /**
     * OpenSearch default username, when secured
     */
    private static final String OPENSEARCH_USERNAME = "admin";

    /**
     * OpenSearch default password, when secured
     */
    private static final String OPENSEARCH_PASSWORD = "admin";

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

    @Test
    public void openSearchDefaultTest() throws IOException {
        // Create the OpenSearch container.
        try (
            OpenSearchContainer container = new OpenSearchContainer(OPENSEARCH_IMAGE).withEnv("foo", "bar") // dummy env for compiler checking correct generics usage
        ) {
            // Start the container. This step might take some time...
            container.start();

            // Do whatever you want with the rest client ...
            Response response = getClient(container).performRequest(new Request("GET", "/"));
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            assertThat(EntityUtils.toString(response.getEntity()), containsString(OPENSEARCH_VERSION));
        }
    }

    @Test
    public void openSearchVersion() throws IOException {
        try (OpenSearchContainer container = new OpenSearchContainer(OPENSEARCH_IMAGE)) {
            container.start();
            Response response = getClient(container).performRequest(new Request("GET", "/"));
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            String responseAsString = EntityUtils.toString(response.getEntity());
            assertThat(responseAsString, containsString(OPENSEARCH_VERSION));
        }
    }

    @Test
    public void openSearchVersion21() throws IOException {
        try (
            OpenSearchContainer container = new OpenSearchContainer(
                "opensearchproject/opensearch:2.1.0"
            )
        ) {
            container.start();
            Response response = getClient(container).performRequest(new Request("GET", "/"));
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            assertThat(EntityUtils.toString(response.getEntity()), containsString("2.1.0"));
        }
    }

    @Test
    public void testOpenSearchSecureByDefault() throws Exception {
        try (
            OpenSearchContainer container = new OpenSearchContainer(
                "opensearchproject/opensearch:2.1.0"
            )
        ) {
            // Start the container. This step might take some time...
            container.start();

            Response response = getClusterHealth(container);
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            assertThat(EntityUtils.toString(response.getEntity()), containsString("cluster_name"));
        }
    }

    @Test
    public void testOpenSearch8SecureByDefaultCustomCaCertFails() {
        final MountableFile mountableFile = MountableFile.forClasspathResource("http_ca.crt");
        String caPath = "/tmp/http_ca.crt";
        try (
            OpenSearchContainer container = new OpenSearchContainer(
                "opensearchproject/opensearch:2.1.0"
            )
                .withCopyToContainer(mountableFile, caPath)
                .withCertPath(caPath)
        ) {
            container.start();

            // this is expected, as a different cert is used for creating the SSL context
            assertThrows(
                "PKIX path validation failed: java.security.cert.CertPathValidatorException: Path does not chain with any of the trust anchors",
                SSLHandshakeException.class,
                () -> getClusterHealth(container)
            );
        }
    }

    @Test
    public void testOpenSearchSecureByDefaultHttpWaitStrategy() throws Exception {
        final HttpWaitStrategy httpsWaitStrategy = Wait
            .forHttps("/")
            .forPort(9200)
            .forStatusCode(200)
            .withBasicCredentials(OPENSEARCH_USERNAME, OPENSEARCH_PASSWORD)
            // trusting self-signed certificate
            .allowInsecure();

        try (
            OpenSearchContainer container = new OpenSearchContainer(
                "opensearchproject/opensearch:2.1.0"
            )
                .waitingFor(httpsWaitStrategy)
        ) {
            // Start the container. This step might take some time...
            container.start();

            Response response = getClusterHealth(container);
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            assertThat(EntityUtils.toString(response.getEntity()), containsString("cluster_name"));
        }
    }

    @Test
    public void testOpenSearchSecureByDefaultFailsSilentlyOnLatestImages() throws Exception {
        tagImage("opensearchproject/opensearch:1.3.4", "opensearch-tc-older-release", "latest");
        DockerImageName image = DockerImageName
            .parse("opensearch-tc-older-release:latest")
            .asCompatibleSubstituteFor("opensearchproject/opensearch");

        try (OpenSearchContainer container = new OpenSearchContainer(image)) {
            container.start();

            Response response = getClient(container).performRequest(new Request("GET", "/_cluster/health"));
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            assertThat(EntityUtils.toString(response.getEntity()), containsString("cluster_name"));
        }
    }

    private void tagImage(String sourceImage, String targetImage, String targetTag) throws IOException {
        try (DockerClient dockerClient = DockerClientFactory.instance().client()) {
            dockerClient
                .tagImageCmd(new RemoteDockerImage(DockerImageName.parse(sourceImage)).get(), targetImage, targetTag)
                .exec();
        }
    }

    private Response getClusterHealth(OpenSearchContainer container) throws IOException {
        // Create the secured client.
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
            AuthScope.ANY,
            new UsernamePasswordCredentials(
                OPENSEARCH_USERNAME,
                OpenSearchContainer.OPENSEARCH_DEFAULT_PASSWORD
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

    private RestClient getClient(OpenSearchContainer container) {
        if (client == null) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(OPENSEARCH_USERNAME, OPENSEARCH_PASSWORD)
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
}
