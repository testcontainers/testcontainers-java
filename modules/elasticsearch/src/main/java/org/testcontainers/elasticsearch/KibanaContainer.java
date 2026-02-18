package org.testcontainers.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Testcontainers implementation for Kibana.
 *  Minimum supported version: {@value MINIMUM_SUPPORTED_VERSION}
 * <p>
 * Supports two modes:
 * <ul>
 *   <li><b>Managed mode:</b> Kibana automatically connects to an {@link ElasticsearchContainer}.
 *       See KibanaContainerTest#managedModeCanStartAndReachElasticsearchInSameExplicitNetwork()</li>
 *   <li><b>External mode:</b> Kibana connects to an external Elasticsearch instance via URL.
 *       See KibanaContainerTest#externalModeCanWorkWithUsernamePassword()</li>
 * </ul>
 * <p>
 */
public class KibanaContainer extends GenericContainer<KibanaContainer> {

    public static final String ES_CA_CERT_PATH = "/usr/share/kibana/config/certs/es-ca.crt";

    private static final Logger log = LoggerFactory.getLogger(KibanaContainer.class);

    public static final int KIBANA_DEFAULT_PORT = 5601;

    public static final String KIBANA_SYSTEM_USER = "kibana_system";

    public static final String MINIMUM_SUPPORTED_VERSION = "8.0.0";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("docker.elastic.co/kibana/kibana");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ElasticsearchContainer elasticsearch;

    private String elasticsearchUrl;

    private String elasticsearchUsername;

    private String elasticsearchPassword;

    private String elasticsearchServiceAccountToken;

    private byte[] elasticsearchCaCertificate;

    private Duration startupTimeout = Duration.ofSeconds(120);

    /**
     * If KibanaContainer creates an ad-hoc shared network (managed mode, neither container has an explicit network),
     * it owns closing it.
     */
    private Network createdSharedNetwork;

    /**
     * Creates a KibanaContainer in managed mode.
     * Kibana automatically connects to the provided Elasticsearch container.
     *
     * @param elasticsearch the Elasticsearch container to connect to
     */
    public KibanaContainer(ElasticsearchContainer elasticsearch) {
        this(buildDockerImageName(elasticsearch));
        this.elasticsearch = elasticsearch;
        dependsOn(elasticsearch);
    }

    /**
     * Creates a KibanaContainer in external mode.
     * Use {@link #withElasticsearchUrl(String)} to configure the Elasticsearch connection. Use other methods to provide security credentials and such.
     *
     * @param dockerImageName the Docker image name
     */
    public KibanaContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * Creates a KibanaContainer in external mode.
     * Use {@link #withElasticsearchUrl(String)} to configure the Elasticsearch connection. Use other methods to provide security credentials and such.
     *
     * @param dockerImageName the Docker image name
     */
    public KibanaContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        ensureCompatibleVersion(dockerImageName.getVersionPart());
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(KIBANA_DEFAULT_PORT);
        //we have to explicitly set wait the strategy later on in configure, once we know the security configuration
        setWaitStrategy(null);
    }

    /**
     * Configures the Elasticsearch URL for external mode.
     *
     * @param elasticsearchUrl the Elasticsearch URL (e.g., "https://my.fancy.setup.elastic.cloud:9200")
     * @return this container instance
     * @throws IllegalStateException if already using managed mode
     */
    public KibanaContainer withElasticsearchUrl(String elasticsearchUrl) {
        if (elasticsearch != null) {
            throw new IllegalStateException("Cannot set Elasticsearch URL when using Elasticsearch container");
        }
        this.elasticsearchUrl = elasticsearchUrl;
        return this;
    }

    /**
     * Configures Kibana to authenticate using the kibana_system user.
     *
     * @param password the password for the kibana_system user
     * @return this container instance
     */
    public KibanaContainer withElasticsearchKibanaSystemPassword(String password) {
        return withElasticsearchCredentials(KIBANA_SYSTEM_USER, password);
    }

    /**
     * Configures Elasticsearch credentials for authentication.
     *
     * @param username the Elasticsearch username (cannot be 'elastic')
     * @param password the password
     * @return this container instance
     * @throws IllegalStateException if a service account token is already configured
     * @throws IllegalArgumentException if credentials are invalid
     */
    public KibanaContainer withElasticsearchCredentials(String username, String password) {
        if (elasticsearchServiceAccountToken != null) {
            throw new IllegalStateException(
                "Conflicting Elasticsearch credentials: provide either a service account token " +
                "or a username/password pair, not both."
            );
        }
        if (StringUtils.isAnyBlank(username, password)) {
            throw new IllegalArgumentException("Kibana credentials cannot be blank");
        }
        if (!username.equals(username.trim()) || !password.equals(password.trim())) {
            throw new IllegalArgumentException("Kibana credentials cannot have leading or trailing whitespace");
        }
        if ("elastic".equals(username)) {
            throw new IllegalArgumentException("Username 'elastic' is reserved for internal use by Elasticsearch");
        }

        this.elasticsearchUsername = username;
        this.elasticsearchPassword = password;
        return this;
    }

    /**
     * Configures a service account token for Elasticsearch authentication.
     *
     * @param token the service account token
     * @return this container instance
     * @throws IllegalStateException if username/password credentials are already configured
     * @throws IllegalArgumentException if token is blank
     */
    public KibanaContainer withElasticsearchServiceAccountToken(String token) {
        if (elasticsearchUsername != null) {
            throw new IllegalStateException(
                "Conflicting Elasticsearch credentials: provide either a service account token " +
                "or a username/password pair, not both."
            );
        }
        if (StringUtils.isBlank(token)) {
            throw new IllegalArgumentException("Service account token cannot be empty");
        }

        if (!token.equals(token.trim())) {
            throw new IllegalArgumentException("Service token cannot have leading or trailing whitespace");
        }
        this.elasticsearchServiceAccountToken = token;
        return this;
    }

    /**
     * Configures the Elasticsearch CA certificate for HTTPS connections.
     *
     * @param caCertificate the CA certificate in PEM format
     * @return this container instance
     * @throws IllegalArgumentException if certificate is empty
     */
    public KibanaContainer withElasticsearchCaCertificate(byte[] caCertificate) {
        if (caCertificate == null || caCertificate.length == 0) {
            throw new IllegalArgumentException("Elasticsearch CA certificate cannot be empty");
        }
        this.elasticsearchCaCertificate = caCertificate;
        return this;
    }

    @Override
    protected void configure() {
        super.configure();

        addEnv("XPACK_ENCRYPTEDSAVEDOBJECTS_ENCRYPTIONKEY", generateRandomKey(32));
        addEnv("SERVER_NAME", "kibana");

        if (elasticsearchCaCertificate != null) {
            withCopyToContainer(Transferable.of(elasticsearchCaCertificate), ES_CA_CERT_PATH);
            addEnv("ELASTICSEARCH_SSL_CERTIFICATEAUTHORITIES", ES_CA_CERT_PATH);
        }
        if (elasticsearch != null) {
            configureManagedElasticsearch();
        } else if (elasticsearchUrl != null) {
            configureExternalElasticsearch();
        } else {
            throw new IllegalStateException(
                "Elasticsearch must be configured either via constructor KibanaContainer(elasticsearch) " +
                "or via .withElasticsearchUrl() for external Elasticsearch"
            );
        }
        //wait strategy is set in configure, because we don't know the security configuration before
        configureWaitStrategy();
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        log.info("Kibana is now ready, it can be accessed at http://{}", getHttpHostAddress());
    }

    @Override
    public void stop() {
        super.stop();
        if (createdSharedNetwork != null) {
            try {
                createdSharedNetwork.close();
            } catch (Exception e) {
                log.debug("Failed to close shared network", e);
            } finally {
                createdSharedNetwork = null;
            }
        }
    }

    @Override
    public KibanaContainer withStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
        return this;
    }

    private static DockerImageName buildDockerImageName(ElasticsearchContainer elasticsearch) {
        String esVersion = DockerImageName.parse(elasticsearch.getDockerImageName()).getVersionPart();
        ensureCompatibleVersion(esVersion);
        return DEFAULT_IMAGE_NAME.withTag(esVersion);
    }

    private void configureExternalElasticsearch() {
        addEnv("ELASTICSEARCH_HOSTS", elasticsearchUrl);
        if (elasticsearchServiceAccountToken != null) {
            addEnv("ELASTICSEARCH_SERVICEACCOUNTTOKEN", elasticsearchServiceAccountToken);
        } else if (elasticsearchUsername != null && elasticsearchPassword != null) {
            addEnv("ELASTICSEARCH_USERNAME", elasticsearchUsername);
            addEnv("ELASTICSEARCH_PASSWORD", elasticsearchPassword);
        } else {
            log.info(
                "No Elasticsearch credentials provided for external mode; Kibana will attempt to connect anonymously"
            );
        }
    }

    private void configureManagedElasticsearch() {
        ensureCorrectNetworkSetupForManagedMode();

        if (getNetwork() == null) {
            createAdHocNetwork();
        }

        String protocol = elasticsearch.getHttpScheme();

        String hosts = protocol + "://" + resolveExistingEsDnsNameOnNetwork(getNetwork()) + ":9200";
        addEnv("ELASTICSEARCH_HOSTS", hosts);

        if ("https".equals(protocol)) {
            // In managed mode, if Elasticsearch uses HTTPS we must configure Kibana with the ES CA, unless provided by user
            if (this.elasticsearchCaCertificate == null) {
                byte[] ca = copyElasticsearchHttpCaCertificateOrThrow();

                withCopyToContainer(Transferable.of(ca), ES_CA_CERT_PATH);
                addEnv("ELASTICSEARCH_SSL_CERTIFICATEAUTHORITIES", ES_CA_CERT_PATH);
            }
            if (elasticsearch != null && !getEnvMap().containsKey("ELASTICSEARCH_SSL_VERIFICATIONMODE")) {
                addEnv("ELASTICSEARCH_SSL_VERIFICATIONMODE", "certificate");
            }
        }

        // Elasticsearch 8.x+ has the security enabled by default, so lack of the env var set to false means security is enabled
        boolean securityDisabled = "false".equalsIgnoreCase(elasticsearch.getEnvMap().get("xpack.security.enabled"));

        if (!securityDisabled) {
            // Managed mode: authenticate Kibana -> Elasticsearch using a Kibana service account token.
            // This avoids any password lifecycle management for kibana_system.
            String token = createKibanaServiceAccountToken(protocol);
            addEnv("ELASTICSEARCH_SERVICEACCOUNTTOKEN", token);
        }
    }

    private byte[] copyElasticsearchHttpCaCertificateOrThrow() {
        try {
            return elasticsearch.copyFileFromContainer(elasticsearch.getCertPath(), IOUtils::toByteArray);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to copy Elasticsearch HTTP CA certificate from '" +
                elasticsearch.getCertPath() +
                "'. " +
                "In managed HTTPS mode, KibanaContainer requires access to the Elasticsearch HTTP CA.",
                e
            );
        }
    }

    private void ensureCorrectNetworkSetupForManagedMode() {
        Network esNetwork = elasticsearch.getNetwork();
        Network kbNetwork = this.getNetwork();

        if ((esNetwork == null) != (kbNetwork == null)) {
            throw new IllegalStateException(
                "Managed mode requires either both containers share the same explicit network, " +
                "or neither specifies a network (KibanaContainer will create one). "
            );
        }

        // Both explicit: must be same
        if (esNetwork != kbNetwork) {
            throw new IllegalStateException(
                "Elasticsearch and Kibana have different networks configured. " +
                "In managed mode both containers must share the same explicit network instance, " +
                "or neither must define a network."
            );
        }
    }

    private void createAdHocNetwork() {
        // Fully managed: create ad-hoc network and own it.
        createdSharedNetwork = Network.newNetwork();
        withNetwork(createdSharedNetwork);

        // Managed-mode safety rule: by the time Kibana is configuring itself, Elasticsearch must already be
        // started (via dependsOn)
        String esId = requireElasticsearchContainerId();

        // Elasticsearch is already created/started. Attach it to the ad-hoc network.
        // We don't need to provide an explicit alias - we'll use the container name for DNS resolution.
        // Equivalent of https://docs.docker.com/reference/cli/docker/network/connect/
        connectRunningContainerToNetwork(esId, createdSharedNetwork);
    }

    private String resolveExistingEsDnsNameOnNetwork(Network network) {
        String esId = requireElasticsearchContainerId();

        InspectContainerResponse info = DockerClientFactory.instance().client().inspectContainerCmd(esId).exec();

        Map<String, ContainerNetwork> networks = info.getNetworkSettings().getNetworks();
        if (networks == null) {
            throw new IllegalStateException("Elasticsearch container has no network configuration");
        }

        // Try to find the network endpoint - Docker may key by network name or ID
        ContainerNetwork endpoint = findNetworkEndpoint(networks, network);
        if (endpoint == null) {
            throw new IllegalStateException(
                "Elasticsearch container is not connected to the expected network. " +
                "Ensure both containers use the same Network instance."
            );
        }

        // Prefer user-defined network aliases (skip Testcontainers auto-generated tc-* aliases)
        if (endpoint.getAliases() != null && !endpoint.getAliases().isEmpty()) {
            for (String alias : endpoint.getAliases()) {
                if (StringUtils.isNotBlank(alias)) {
                    String cleaned = alias.trim();
                    // Skip Testcontainers auto-generated aliases (tc-*), prefer user-defined ones
                    if (!cleaned.startsWith("tc-")) {
                        log.info("Using Elasticsearch network alias: {}", cleaned);
                        return cleaned;
                    }
                }
            }
        }

        // Fallback: use container name
        String containerName = info.getName();
        if (containerName != null && !containerName.trim().isEmpty()) {
            String dnsName = containerName.replaceFirst("^/", "").trim();
            log.info("No user-defined network alias found, using Elasticsearch container name: {}", dnsName);
            return dnsName;
        }

        throw new IllegalStateException(
            "Cannot determine Elasticsearch DNS name. " +
            "When using a custom network, set a network alias on the Elasticsearch container."
        );
    }

    private ContainerNetwork findNetworkEndpoint(Map<String, ContainerNetwork> networks, Network network) {
        // Try network ID first
        ContainerNetwork endpoint = networks.get(network.getId());
        if (endpoint != null) {
            return endpoint;
        }

        // Try network name as fallback (Docker may key by name instead of ID)
        try {
            String networkName = DockerClientFactory
                .instance()
                .client()
                .inspectNetworkCmd()
                .withNetworkId(network.getId())
                .exec()
                .getName();
            if (networkName != null) {
                return networks.get(networkName);
            }
        } catch (Exception e) {
            // Ignore and return null
        }

        return null;
    }

    private String requireElasticsearchContainerId() {
        String id = elasticsearch.getContainerId();
        if (StringUtils.isBlank(id)) {
            throw new IllegalStateException(
                "Elasticsearch containerId is not available. In managed mode, Elasticsearch must be started via dependsOn(elasticsearch) " +
                "before KibanaContainer is started."
            );
        }
        return id.trim();
    }

    private void connectRunningContainerToNetwork(String containerId, Network network) {
        String networkId = network.getId();

        try {
            DockerClientFactory
                .instance()
                .client()
                .connectToNetworkCmd()
                .withContainerId(containerId)
                .withNetworkId(networkId)
                .exec();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to connect Elasticsearch container to ad-hoc shared network", e);
        }
    }

    private static void ensureCompatibleVersion(String esVersion) {
        ComparableVersion comparableVersion = new ComparableVersion(esVersion);
        if (comparableVersion.isLessThan(MINIMUM_SUPPORTED_VERSION)) {
            throw new IllegalArgumentException(
                String.format(
                    "Kibana version %s is not supported. Minimum version is %s",
                    comparableVersion,
                    MINIMUM_SUPPORTED_VERSION
                )
            );
        }
    }

    private String createKibanaServiceAccountToken(String protocol) {
        if (elasticsearch == null) {
            throw new IllegalStateException("Cannot create service account token in external mode");
        }

        String elasticPassword = elasticsearch
            .getEnvMap()
            .getOrDefault("ELASTIC_PASSWORD", ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD);

        // Create a unique token name to avoid collisions if the same ES container is reused.
        String tokenName = "tc-kibana-" + Base58.randomString(12);

        String endpoint = protocol + "://localhost:9200/_security/service/elastic/kibana/credential/token/" + tokenName;

        return Unreliables.retryUntilSuccess(
            45,
            TimeUnit.SECONDS,
            () -> {
                String curlTlsArgs = "";
                if ("https".equals(protocol)) {
                    // In managed HTTPS mode, use the Elasticsearch HTTP CA for curl.
                    curlTlsArgs = " --cacert '" + elasticsearch.getCertPath() + "'";
                }

                String curlCommand = String.format(
                    "curl -sS%s -u \"elastic:$1\" -H 'Content-Type: application/json' -X POST '%s'",
                    curlTlsArgs,
                    endpoint
                );

                Container.ExecResult result = elasticsearch.execInContainer(
                    "/bin/sh",
                    "-c",
                    curlCommand,
                    "sh",
                    elasticPassword
                );

                String stdout = (result.getStdout() == null) ? "" : result.getStdout();
                String stderr = (result.getStderr() == null) ? "" : result.getStderr();

                if (result.getExitCode() != 0) {
                    throw new RuntimeException(
                        "Failed to create Kibana service account token. Exit code: " +
                        result.getExitCode() +
                        ", stdout: " +
                        stdout +
                        ", stderr: " +
                        stderr
                    );
                }

                JsonNode json = OBJECT_MAPPER.readTree(stdout);
                JsonNode value = json.path("token").path("value");
                if (value.isTextual() && !value.asText().trim().isEmpty()) {
                    return value.asText().trim();
                }

                throw new RuntimeException("Service account token response did not contain token.value: " + stdout);
            }
        );
    }

    private String generateRandomKey(int length) {
        return Base58.randomString(length);
    }

    /**
     * Returns the HTTP host address for accessing Kibana.
     *
     * @return the host address in the format "host:port"
     */
    public String getHttpHostAddress() {
        return getHost() + ":" + getMappedPort(KIBANA_DEFAULT_PORT);
    }

    private void configureWaitStrategy() {
        if (this.getWaitStrategy() != null) {
            // the user might have set a custom wait strategy
            return;
        }
        HttpWaitStrategy strategy = Wait
            .forHttp("/api/status")
            .forPort(KIBANA_DEFAULT_PORT)
            .forStatusCode(200)
            .forResponsePredicate(this::isKibanaReady);

        // Add authentication if we have Elasticsearch credentials available
        String serviceToken = getEnvMap().get("ELASTICSEARCH_SERVICEACCOUNTTOKEN");
        String username = getEnvMap().get("ELASTICSEARCH_USERNAME");
        String password = getEnvMap().get("ELASTICSEARCH_PASSWORD");

        if (serviceToken != null) {
            strategy = strategy.withHeader("Authorization", "Bearer " + serviceToken);
        } else if (username != null && password != null) {
            strategy = strategy.withBasicCredentials(username, password);
        }

        setWaitStrategy(strategy.withStartupTimeout(this.startupTimeout));
    }

    private boolean isKibanaReady(String body) {
        try {
            JsonNode json = OBJECT_MAPPER.readTree(body);
            JsonNode status = json.path("status");

            String overallLevel = status.path("overall").path("level").asText(null);
            String elasticsearchLevel = status.path("core").path("elasticsearch").path("level").asText(null);
            String savedObjectsLevel = status.path("core").path("savedObjects").path("level").asText(null);

            boolean overallAvailable = "available".equalsIgnoreCase(overallLevel);
            boolean elasticsearchAvailable = "available".equalsIgnoreCase(elasticsearchLevel);
            boolean savedObjectsAvailable = "available".equalsIgnoreCase(savedObjectsLevel);

            boolean isReady = overallAvailable && elasticsearchAvailable && savedObjectsAvailable;

            if (log.isDebugEnabled()) {
                log.debug(
                    "Kibana status check: READY={} (overall={}, elasticsearch={}, savedObjects={})",
                    isReady,
                    overallLevel,
                    elasticsearchLevel,
                    savedObjectsLevel
                );
            }

            return isReady;
        } catch (Exception e) {
            log.debug("Kibana status check: FAILED to parse response - {}", e.getMessage());
            return false;
        }
    }
}
