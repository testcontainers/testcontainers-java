package org.testcontainers.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.testcontainers.elasticsearch.ElasticsearchContainerTest.ELASTICSEARCH_IMAGE_LATEST;
import static org.testcontainers.elasticsearch.ElasticsearchContainerTest.ELASTICSEARCH_VERSION_7;
import static org.testcontainers.elasticsearch.ElasticsearchContainerTest.ELASTICSEARCH_VERSION_LATEST;

class KibanaContainerTest {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String KIBANA_IMAGE = "docker.elastic.co/kibana/kibana:" + ELASTICSEARCH_VERSION_LATEST;

    /**
     * A Kibana patch intentionally behind the latest Elasticsearch version, to verify mixed-patch compatibility.
     */
    private static final String KIBANA_IMAGE_OLDER_PATCH = "docker.elastic.co/kibana/kibana:9.5.0";

    // Managed mode (latest)

    @Test
    void managedModeReachesElasticsearchOnSharedNetwork() throws IOException {
        // managedModeReachesElasticsearchOnSharedNetwork {
        try (
            Network network = Network.newNetwork();
            ElasticsearchContainer es = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST).withNetwork(network);
            KibanaContainer kibana = new KibanaContainer(es).withNetwork(network)
        ) {
            es.start();
            kibana.start();

            assertKibanaIsAvailable(kibana);
        }
        // }
    }

    @Test
    void managedModeReachesElasticsearchWithoutExplicitNetwork() throws IOException {
        try (
            ElasticsearchContainer es = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST);
            KibanaContainer kibana = new KibanaContainer(es)
        ) {
            kibana.start();

            assertKibanaIsAvailable(kibana);
        }
    }

    @Test
    void managedModeWorksWhenElasticsearchSecurityIsDisabled() throws IOException {
        try (
            ElasticsearchContainer es = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST)
                .withEnv("xpack.security.enabled", "false");
            KibanaContainer kibana = new KibanaContainer(es)
        ) {
            kibana.start();

            assertKibanaIsAvailable(kibana);
        }
    }

    @Test
    void managedModeWorksWhenElasticsearchTlsIsDisabled() throws IOException {
        try (
            ElasticsearchContainer es = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST)
                .withEnv("xpack.security.enabled", "true")
                .withEnv("xpack.security.http.ssl.enabled", "false");
            KibanaContainer kibana = new KibanaContainer(es)
        ) {
            es.start();
            kibana.start();

            assertKibanaIsAvailable(kibana);
        }
    }

    @Test
    void managedModeUsesCustomElasticsearchNetworkAlias() throws Exception {
        final String customEsAlias = "my-custom-es-alias";

        try (
            Network network = Network.newNetwork();
            ElasticsearchContainer es = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST)
                .withNetwork(network)
                .withNetworkAliases(customEsAlias);
            KibanaContainer kibana = new KibanaContainer(es).withNetwork(network)
        ) {
            kibana.start();

            assertThat(kibana.isRunning()).as("Kibana container is running").isTrue();

            Container.ExecResult result = kibana.execInContainer("sh", "-c", "env | grep ELASTICSEARCH_HOSTS");

            assertThat(result.getStdout())
                .as("ELASTICSEARCH_HOSTS uses the custom Elasticsearch network alias")
                .contains(customEsAlias)
                .contains(":9200");
        }
    }

    @Test
    void managedModeRejectsWhenOnlyElasticsearchHasExplicitNetwork() {
        Network network = Network.newNetwork();
        try (
            ElasticsearchContainer es = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST).withNetwork(network);
            KibanaContainer kibana = new KibanaContainer(es)
        ) {
            assertThatThrownBy(kibana::start)
                .as("managed mode requires Kibana to join the same explicit network as Elasticsearch")
                .isInstanceOf(ContainerLaunchException.class)
                .satisfies(ex -> {
                    assertThat(ex.getCause())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("explicit network");
                });
        }
    }

    @Test
    void managedModeRejectsWhenNetworksDiffer() {
        try (
            Network esNetwork = Network.newNetwork();
            Network kibanaNetwork = Network.newNetwork();
            ElasticsearchContainer es = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST).withNetwork(esNetwork);
            KibanaContainer kibana = new KibanaContainer(es).withNetwork(kibanaNetwork)
        ) {
            assertThatThrownBy(kibana::start)
                .as("managed mode rejects Kibana and Elasticsearch on different networks")
                .isInstanceOf(ContainerLaunchException.class)
                .satisfies(ex -> {
                    assertThat(ex.getCause())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("different networks");
                });
        }
    }

    @Test
    void managedModeRejectsExplicitElasticsearchUrl() {
        ElasticsearchContainer es = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST);
        assertThatThrownBy(() -> {
            new KibanaContainer(es).withElasticsearchUrl("http://somewhere.over.the.rainbow:9200");
        })
            .as("managed mode cannot also set an explicit Elasticsearch URL")
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot set Elasticsearch URL when using Elasticsearch container");
    }

    // External mode (latest)

    @Test
    void externalModeReachesElasticsearchWithUsernamePassword() throws IOException {
        final String esHostname = "elasticsearch";

        // externalModeReachesElasticsearchWithUsernamePassword {
        try (
            Network network = Network.newNetwork();
            ElasticsearchContainer es = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST)
                .withNetwork(network)
                .withNetworkAliases(esHostname)
                .withEnv("xpack.security.http.ssl.enabled", "false")
        ) {
            es.start();
            String kibanaSystemPassword = setKibanaSystemPassword(es);

            try (
                KibanaContainer kibana = new KibanaContainer(KIBANA_IMAGE)
                    .withNetwork(network)
                    .withElasticsearchUrl("http://" + esHostname + ":9200")
                    .withKibanaSystemPassword(kibanaSystemPassword)
            ) {
                kibana.start();
                assertKibanaIsAvailable(kibana);
            }
        }
        // }
    }

    @Test
    void externalModeReachesElasticsearchWithoutCredentials() throws IOException {
        final String esHostname = "elasticsearch";

        try (
            Network network = Network.newNetwork();
            ElasticsearchContainer es = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST)
                .withNetwork(network)
                .withNetworkAliases(esHostname)
                .withEnv("xpack.security.enabled", "false")
                .withEnv("xpack.security.http.ssl.enabled", "false");
            KibanaContainer kibana = new KibanaContainer(KIBANA_IMAGE)
                .withNetwork(network)
                .withElasticsearchUrl("http://" + esHostname + ":9200")
        ) {
            es.start();
            kibana.start();
            assertKibanaIsAvailable(kibana);
        }
    }

    @Test
    void externalModeReachesElasticsearchWithTlsAndServiceToken() throws Exception {
        byte[] caCrt;
        byte[] nodeCrt;
        byte[] nodeKey;

        String esHostname = "elasticsearch";

        String instancesYml =
            "instances:\n" +
            "  - name: es01\n" +
            "    dns: [ \"localhost\", \"" +
            esHostname +
            "\", \"es01\" ]\n" +
            "    ip:  [ \"127.0.0.1\" ]\n";

        try (
            ElasticsearchContainer setup = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST)
                .withEnv("discovery.type", "single-node")
                .withCopyToContainer(
                    Transferable.of(instancesYml.getBytes(StandardCharsets.UTF_8), 0644),
                    "/tmp/instances.yml"
                )
        ) {
            setup.start();

            Container.ExecResult execResult = setup.execInContainer(
                "bash",
                "-lc",
                "set -euo pipefail && " +
                "mkdir -p /tmp/out && " +
                "cd /usr/share/elasticsearch && " +
                "bin/elasticsearch-certutil ca --silent --pem --out /tmp/out/ca.zip && " +
                "unzip -o /tmp/out/ca.zip -d /tmp/out && " +
                "bin/elasticsearch-certutil cert --silent --pem --in /tmp/instances.yml --ca-cert /tmp/out/ca/ca.crt --ca-key /tmp/out/ca/ca.key --out /tmp/out/certs.zip && " +
                "unzip -o /tmp/out/certs.zip -d /tmp/out"
            );
            assertThat(execResult.getExitCode()).as("elasticsearch-certutil generated certificates").isEqualTo(0);

            caCrt = setup.copyFileFromContainer("/tmp/out/ca/ca.crt", IOUtils::toByteArray);
            nodeCrt = setup.copyFileFromContainer("/tmp/out/es01/es01.crt", IOUtils::toByteArray);
            nodeKey = setup.copyFileFromContainer("/tmp/out/es01/es01.key", IOUtils::toByteArray);
        }

        try (
            Network network = Network.newNetwork();
            ElasticsearchContainer es = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST)
                .withNetwork(network)
                .withNetworkAliases(esHostname)
        ) {
            applyTls(es, caCrt, nodeCrt, nodeKey);
            es.start();
            String kibanaServiceAccountToken = createKibanaServiceAccountToken(es);

            try (
                KibanaContainer kibana = new KibanaContainer(KIBANA_IMAGE)
                    // network is needed only because the ES we try to access via explicit mode is operated by non-public Docker
                    .withNetwork(network)
                    .withElasticsearchUrl("https://" + esHostname + ":9200")
                    .withElasticsearchServiceAccountToken(kibanaServiceAccountToken)
                    .withElasticsearchCaCertificate(es.caCertAsBytes().get())
            ) {
                kibana.start();
                assertKibanaIsAvailable(kibana);
            }
        }
    }

    @Test
    void externalModeRejectsConflictingCredentials() {
        assertThatThrownBy(() -> {
            new KibanaContainer(KIBANA_IMAGE)
                .withKibanaUsernameAndPassword("user", "pass")
                .withElasticsearchServiceAccountToken("token");
        })
            .as("username/password and a service account token cannot be set together")
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Conflicting Elasticsearch credentials");
    }

    @Test
    void startFailsWhenElasticsearchIsNotConfigured() {
        try (KibanaContainer kibana = new KibanaContainer(KIBANA_IMAGE)) {
            assertThatThrownBy(kibana::start)
                .as("Kibana cannot start without an Elasticsearch URL or container")
                .isInstanceOf(ContainerLaunchException.class)
                .satisfies(ex -> {
                    assertThat(ex.getCause())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Elasticsearch must be configured");
                });
        }
    }

    @Test
    void reusesContainerWhenReuseIsEnabled() {
        assumeThat(TestcontainersConfiguration.getInstance().environmentSupportsReuse())
            .as("testcontainers.reuse.enable must be true")
            .isTrue();

        // Testcontainers.exposeHostPorts + host.testcontainers.internal lets Kibana reach ES
        // from inside the container on any platform (Linux Docker Engine included).
        // No withNetwork() on Kibana keeps the hash fully deterministic:
        //   - no dynamic network ID
        //   - the random tc-* alias added by GenericContainer's constructor is only serialised
        //     into the CreateContainerCmd when withNetwork() has been called, so it is absent here
        // The host.testcontainers.internal extra-host IP is the same for kibana1 and kibana2
        // because they start in the same JVM (same PortForwardingContainer instance).
        // The first Kibana container must stay running while the second one starts, because
        // withReuse(true) only skips JVM-shutdown cleanup — an explicit stop() still removes the
        // container, so there would be nothing to find.
        try (
            ElasticsearchContainer es = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST)
                .withEnv("xpack.security.enabled", "false")
                .withEnv("xpack.security.http.ssl.enabled", "false")
        ) {
            es.start();
            int esMappedPort = es.getMappedPort(9200);
            Testcontainers.exposeHostPorts(esMappedPort);
            String esUrl = "http://" + GenericContainer.INTERNAL_HOST_HOSTNAME + ":" + esMappedPort;

            KibanaContainer kibana1 = new KibanaContainer(KIBANA_IMAGE).withElasticsearchUrl(esUrl).withReuse(true);
            KibanaContainer kibana2 = new KibanaContainer(KIBANA_IMAGE).withElasticsearchUrl(esUrl).withReuse(true);

            try {
                kibana1.start();
                // kibana2 is started while kibana1 is still running; the reuse mechanism should
                // find kibana1's container by hash and return the same container ID.
                kibana2.start();

                assertThat(kibana2.getContainerId())
                    .as("withReuse(true) reuses the same container on subsequent starts")
                    .isEqualTo(kibana1.getContainerId());
            } finally {
                kibana1.stop();
                kibana2.stop();
            }
        }
    }

    // Version-specific

    @Test
    void rejectsKibanaVersionBelow8() {
        assertThatThrownBy(() -> new KibanaContainer("docker.elastic.co/kibana/kibana:" + ELASTICSEARCH_VERSION_7))
            .as("Kibana versions below 8.0.0 are not supported")
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is not supported");
    }

    @Test
    void externalModeAcceptsOlderKibanaPatchThanElasticsearch() throws IOException {
        final String esHostname = "elasticsearch";

        try (
            Network network = Network.newNetwork();
            ElasticsearchContainer es = new ElasticsearchContainer(ELASTICSEARCH_IMAGE_LATEST)
                .withNetwork(network)
                .withNetworkAliases(esHostname)
                .withEnv("xpack.security.http.ssl.enabled", "false")
        ) {
            es.start();
            String kibanaSystemPassword = setKibanaSystemPassword(es);

            try (
                KibanaContainer kibana = new KibanaContainer(KIBANA_IMAGE_OLDER_PATCH)
                    .withNetwork(network)
                    .withElasticsearchUrl("http://" + esHostname + ":9200")
                    .withKibanaSystemPassword(kibanaSystemPassword)
            ) {
                kibana.start();
                assertKibanaIsAvailable(kibana);
            }
        }
    }

    private static void assertKibanaIsAvailable(KibanaContainer kibana) throws IOException {
        assertThat(getKibanaStatus(kibana)).as("Kibana reports overall status available").isEqualTo("available");
    }

    private static String setKibanaSystemPassword(ElasticsearchContainer elasticsearch) throws IOException {
        String kibanaPassword = "kibana-system-" + System.currentTimeMillis();

        try (CloseableHttpClient httpClient = createHttpClient(elasticsearch)) {
            String url = String.format(
                "%s://%s/_security/user/kibana_system/_password",
                elasticsearch.getHttpScheme(),
                elasticsearch.getHttpHostAddress()
            );
            HttpPost request = new HttpPost(url);
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity("{\"password\":\"" + kibanaPassword + "\"}"));

            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());

            if (statusCode != 200) {
                throw new IllegalStateException(
                    "Failed to set kibana_system password. HTTP " + statusCode + ", body=" + body
                );
            }

            // ES 9.x returns {} on success; older versions may return {"acknowledged":true}
            // Just validate that the body is valid JSON.
            try {
                OBJECT_MAPPER.readTree(body.isEmpty() ? "{}" : body);
            } catch (IOException e) {
                throw new IllegalStateException("Non-JSON response body: " + body, e);
            }

            return kibanaPassword;
        }
    }

    private static String createKibanaServiceAccountToken(ElasticsearchContainer elasticsearch) throws IOException {
        String tokenName = "kibana-token-" + System.currentTimeMillis();

        try (CloseableHttpClient httpClient = createHttpClient(elasticsearch)) {
            String url = String.format(
                "%s://%s/_security/service/elastic/kibana/credential/token/%s",
                elasticsearch.getHttpScheme(),
                elasticsearch.getHttpHostAddress(),
                tokenName
            );
            HttpPost request = new HttpPost(url);
            request.setHeader("Content-Type", "application/json");

            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());

            if (statusCode != 200) {
                throw new IllegalStateException(
                    "Failed to create Kibana service account token. HTTP " + statusCode + ", body=" + body
                );
            }

            // Expected JSON:
            // {"created":true,"token":{"name":"...","value":"AAEAA..."}}
            try {
                JsonNode root = OBJECT_MAPPER.readTree(body);
                JsonNode tokenValue = root.path("token").path("value");

                if (tokenValue.isMissingNode() || tokenValue.isNull()) {
                    throw new IllegalStateException("Token value not found in response: " + body);
                }

                return tokenValue.asText();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to parse token response: " + body, e);
            }
        }
    }

    private static CloseableHttpClient createHttpClient(ElasticsearchContainer elasticsearch) {
        String elasticPassword = elasticsearch.getEnvMap().get("ELASTIC_PASSWORD");
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();

        if (StringUtils.isNotBlank(elasticPassword)) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials("elastic", elasticPassword)
            );
            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }

        String scheme = elasticsearch.getHttpScheme();
        if ("https".equals(scheme)) {
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                elasticsearch.createSslContextFromCa()
            );
            clientBuilder.setSSLSocketFactory(sslSocketFactory);
        }

        return clientBuilder.build();
    }

    private static String getKibanaStatus(KibanaContainer kibana) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            String url = "http://" + kibana.getHttpHostAddress() + "/api/status";
            HttpResponse response = httpClient.execute(new org.apache.http.client.methods.HttpGet(url));
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());

            if (statusCode != 200) {
                throw new IllegalStateException("Failed to get Kibana status. HTTP " + statusCode + ", body=" + body);
            }

            JsonNode json = OBJECT_MAPPER.readTree(body);
            String status = json.path("status").path("overall").path("level").asText(null);
            if (status == null) {
                throw new IllegalStateException("Kibana status response missing 'status.overall.level' field: " + body);
            }
            return status;
        }
    }

    private static void applyTls(ElasticsearchContainer c, byte[] caCrt, byte[] nodeCrt, byte[] nodeKey) {
        final String certDir = "/usr/share/elasticsearch/config/certs";

        c.withCopyToContainer(Transferable.of(caCrt, 0644), certDir + "/http_ca.crt");
        c.withCopyToContainer(Transferable.of(nodeCrt, 0644), certDir + "/http.crt");
        c.withCopyToContainer(Transferable.of(nodeKey, 0644), certDir + "/http.key");

        c.withEnv("xpack.security.autoconfiguration.enabled", "false");
        c.withEnv("xpack.security.enabled", "true");

        c.withEnv("xpack.security.http.ssl.enabled", "true");
        c.withEnv("xpack.security.http.ssl.certificate_authorities", "certs/http_ca.crt");
        c.withEnv("xpack.security.http.ssl.certificate", "certs/http.crt");
        c.withEnv("xpack.security.http.ssl.key", "certs/http.key");
    }
}
