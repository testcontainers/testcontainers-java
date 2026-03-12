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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

class KibanaContainerTest {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String ES_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:9.2.4";

    @Test
    void cannotCreateKibanaContainerForVersionLessThan8() {
        Assertions
            .assertThatThrownBy(() -> new KibanaContainer("docker.elastic.co/kibana/kibana:7.17.29"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("is not supported");
    }

    @Test
    void managedModeCanStartAndReachElasticsearchInSameExplicitNetwork() throws IOException {
        // managedModeCanStartAndReachElasticsearchInSameExplicitNetwork {
        try (
            Network network = Network.newNetwork();
            ElasticsearchContainer es = new ElasticsearchContainer(ES_IMAGE).withNetwork(network);
            KibanaContainer kibana = new KibanaContainer(es).withNetwork(network)
        ) {
            es.start();
            kibana.start();

            String status = getKibanaStatus(kibana);
            Assertions.assertThat(status).isEqualTo("available");
        }
        // }
    }

    @Test
    void managedModeCannotStartWithOnlyESNetworkExplicit() {
        Network network = Network.newNetwork();
        try (
            ElasticsearchContainer es = new ElasticsearchContainer(ES_IMAGE).withNetwork(network);
            KibanaContainer kibana = new KibanaContainer(es)
        ) {
            Assertions
                .assertThatThrownBy(kibana::start)
                .isInstanceOf(ContainerLaunchException.class)
                .satisfies(ex -> {
                    Assertions
                        .assertThat(ex.getCause())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("explicit network");
                });
        }
    }

    @Test
    void managedModeCannotStartWithDifferentExplicitNetworks() {
        try (
            Network esNetwork = Network.newNetwork();
            Network kibanaNetwork = Network.newNetwork();
            ElasticsearchContainer es = new ElasticsearchContainer(ES_IMAGE).withNetwork(esNetwork);
            KibanaContainer kibana = new KibanaContainer(es).withNetwork(kibanaNetwork)
        ) {
            Assertions
                .assertThatThrownBy(kibana::start)
                .isInstanceOf(ContainerLaunchException.class)
                .satisfies(ex -> {
                    Assertions
                        .assertThat(ex.getCause())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("different networks");
                });
        }
    }

    @Test
    void managedModeUsesCustomNetworkAliasInExplicitNetwork() throws Exception {
        final String customEsAlias = "my-custom-es-alias";

        try (
            Network network = Network.newNetwork();
            ElasticsearchContainer es = new ElasticsearchContainer(ES_IMAGE)
                .withNetwork(network)
                .withNetworkAliases(customEsAlias);
            KibanaContainer kibana = new KibanaContainer(es).withNetwork(network)
        ) {
            kibana.start();

            Assertions.assertThat(kibana.isRunning()).isTrue();

            // Verify Kibana uses the custom alias (not auto-generated tc-* alias)
            Container.ExecResult result = kibana.execInContainer("sh", "-c", "env | grep ELASTICSEARCH_HOSTS");

            Assertions.assertThat(result.getStdout()).contains(customEsAlias).contains(":9200");
        }
    }

    @Test
    void managedModeCanStartAndReachElasticsearchWithoutExplicitNetwork() throws IOException {
        try (
            ElasticsearchContainer es = new ElasticsearchContainer(ES_IMAGE);
            KibanaContainer kibana = new KibanaContainer(es)
        ) {
            kibana.start();

            String status = getKibanaStatus(kibana);
            Assertions.assertThat(status).isEqualTo("available");
        }
    }

    @Test
    void managedModeCanStartWithoutElasticsearchSecurity() throws IOException {
        try (
            ElasticsearchContainer es = new ElasticsearchContainer(ES_IMAGE).withEnv("xpack.security.enabled", "false");
            KibanaContainer kibana = new KibanaContainer(es)
        ) {
            kibana.start();

            String status = getKibanaStatus(kibana);
            Assertions.assertThat(status).isEqualTo("available");
        }
    }

    @Test
    void managedModeCanStartWithoutElasticsearchHttps() throws IOException {
        try (
            ElasticsearchContainer es = new ElasticsearchContainer(ES_IMAGE)
                .withEnv("xpack.security.enabled", "true")
                .withEnv("xpack.security.http.ssl.enabled", "false");
            KibanaContainer kibana = new KibanaContainer(es)
        ) {
            es.start();
            kibana.start();

            String status = getKibanaStatus(kibana);
            Assertions.assertThat(status).isEqualTo("available");
        }
    }

    @Test
    void externalModeFailsWithConflictingCredentials() {
        Assertions
            .assertThatThrownBy(() -> {
                new KibanaContainer("docker.elastic.co/kibana/kibana:8.0.0")
                    .withKibanaUsernameAndPassword("user", "pass")
                    .withElasticsearchServiceAccountToken("token");
            })
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Conflicting Elasticsearch credentials");
    }

    @Test
    void managedModeFailsWhenSettingElasticsearchUrl() {
        ElasticsearchContainer es = new ElasticsearchContainer(ES_IMAGE);
        Assertions
            .assertThatThrownBy(() -> {
                new KibanaContainer(es).withElasticsearchUrl("http://somewhere.over.the.rainbow:9200");
            })
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot set Elasticsearch URL when using Elasticsearch container");
    }

    @Test
    void failsWhenNoElasticsearchConfigured() {
        try (KibanaContainer kibana = new KibanaContainer("docker.elastic.co/kibana/kibana:8.0.0")) {
            Assertions
                .assertThatThrownBy(kibana::start)
                .isInstanceOf(ContainerLaunchException.class)
                .satisfies(ex -> {
                    Assertions
                        .assertThat(ex.getCause())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Elasticsearch must be configured");
                });
        }
    }

    @Test
    void externalModeCanWorkWithUsernamePassword() throws IOException, InterruptedException {
        final String esHostname = "elasticsearch";

        // externalModeCanWorkWithUsernamePassword {
        try (
            Network network = Network.newNetwork();
            ElasticsearchContainer es = new ElasticsearchContainer(ES_IMAGE)
                .withNetwork(network)
                .withNetworkAliases(esHostname)
                .withEnv("xpack.security.http.ssl.enabled", "false")
        ) {
            es.start();
            String kibanaSystemPassword = setKibanaSystemPassword(es);

            try (
                KibanaContainer kibana = new KibanaContainer("docker.elastic.co/kibana/kibana:9.2.2") //this minor version is intentionally below ES version
                    .withNetwork(network)
                    .withElasticsearchUrl("http://" + esHostname + ":9200")
                    .withKibanaSystemPassword(kibanaSystemPassword)
            ) {
                kibana.start();
                String status = getKibanaStatus(kibana);
                Assertions.assertThat(status).isEqualTo("available");
            }
        }
        // }
    }

    @Test
    void externalModeCanWorkWithoutCredentials() throws IOException {
        final String esHostname = "elasticsearch";

        try (
            Network network = Network.newNetwork();
            ElasticsearchContainer es = new ElasticsearchContainer(ES_IMAGE)
                .withNetwork(network)
                .withNetworkAliases(esHostname)
                .withEnv("xpack.security.enabled", "false")
                .withEnv("xpack.security.http.ssl.enabled", "false");
            KibanaContainer kibana = new KibanaContainer("docker.elastic.co/kibana/kibana:9.2.4")
                .withNetwork(network)
                .withElasticsearchUrl("http://" + esHostname + ":9200")
        ) {
            es.start();
            kibana.start();
            String status = getKibanaStatus(kibana);
            Assertions.assertThat(status).isEqualTo("available");
        }
    }

    @Test
    void externalModeCanStartAndReachElasticsearchWithCertAndServiceToken() throws Exception {
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
            ElasticsearchContainer setup = new ElasticsearchContainer(ES_IMAGE)
                .withEnv("discovery.type", "single-node")
                .withCopyToContainer(
                    Transferable.of(instancesYml.getBytes(StandardCharsets.UTF_8), 0644),
                    "/tmp/instances.yml"
                )
        ) {
            setup.start();

            // Run certutil inside the running container and write outputs inside the container FS
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
            Assertions.assertThat(execResult.getExitCode()).isEqualTo(0);

            // copy the certificates and key from the container, so we can use them later
            caCrt = setup.copyFileFromContainer("/tmp/out/ca/ca.crt", IOUtils::toByteArray);
            nodeCrt = setup.copyFileFromContainer("/tmp/out/es01/es01.crt", IOUtils::toByteArray);
            nodeKey = setup.copyFileFromContainer("/tmp/out/es01/es01.key", IOUtils::toByteArray);
        }

        try (
            Network network = Network.newNetwork();
            ElasticsearchContainer es = new ElasticsearchContainer(
                "docker.elastic.co/elasticsearch/elasticsearch:9.2.4"
            )
                .withNetwork(network)
                .withNetworkAliases(esHostname)
        ) {
            applyTls(es, caCrt, nodeCrt, nodeKey);
            es.start();
            String kibanaServiceAccountToken = createKibanaServiceAccountToken(es);

            try (
                KibanaContainer kibana = new KibanaContainer("docker.elastic.co/kibana/kibana:9.2.4")
                    // network is needed only because the ES we try to access via explicit mode is operated by non-public Docker
                    .withNetwork(network)
                    .withElasticsearchUrl("https://" + esHostname + ":9200")
                    .withElasticsearchServiceAccountToken(kibanaServiceAccountToken)
                    .withElasticsearchCaCertificate(es.caCertAsBytes().get())
            ) {
                kibana.start();
                String status = getKibanaStatus(kibana);
                Assertions.assertThat(status).isEqualTo("available");
            }
        }
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

        // Copy provided materials
        c.withCopyToContainer(Transferable.of(caCrt, 0644), certDir + "/http_ca.crt");
        c.withCopyToContainer(Transferable.of(nodeCrt, 0644), certDir + "/http.crt");
        c.withCopyToContainer(Transferable.of(nodeKey, 0644), certDir + "/http.key");

        // Disable ES bootstrap TLS autoconfiguration
        c.withEnv("xpack.security.autoconfiguration.enabled", "false");
        c.withEnv("xpack.security.enabled", "true");

        // Configure ONLY HTTP TLS using exactly the provided files
        c.withEnv("xpack.security.http.ssl.enabled", "true");
        c.withEnv("xpack.security.http.ssl.certificate_authorities", "certs/http_ca.crt");
        c.withEnv("xpack.security.http.ssl.certificate", "certs/http.crt");
        c.withEnv("xpack.security.http.ssl.key", "certs/http.key");
    }
}
