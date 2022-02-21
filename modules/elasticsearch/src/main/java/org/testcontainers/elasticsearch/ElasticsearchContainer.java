package org.testcontainers.elasticsearch;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.apache.commons.io.IOUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

/**
 * Represents an elasticsearch docker instance which exposes by default port 9200 and 9300 (transport.tcp.port)
 * The docker image is by default fetched from docker.elastic.co/elasticsearch/elasticsearch
 */
public class ElasticsearchContainer extends GenericContainer<ElasticsearchContainer> {

    /**
     * Elasticsearch Default Password for Elasticsearch &gt;= 8
     */
    public static final String ELASTICSEARCH_DEFAULT_PASSWORD = "changeme";

    /**
     * Elasticsearch Default HTTP port
     */
    private static final int ELASTICSEARCH_DEFAULT_PORT = 9200;

    /**
     * Elasticsearch Default Transport port
     * The TransportClient will be removed in Elasticsearch 8. No need to expose this port anymore in the future.
     */
    @Deprecated
    private static final int ELASTICSEARCH_DEFAULT_TCP_PORT = 9300;

    /**
     * Elasticsearch Docker base image
     */
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch");
    private static final DockerImageName DEFAULT_OSS_IMAGE_NAME = DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch-oss");

    /**
     * Elasticsearch Default version
     */
    @Deprecated
    protected static final String DEFAULT_TAG = "7.9.2";

    // matches 8.x.x, 9.x.x and any double digit version, also supports those -alpha2 suffixes
    private static final Pattern VERSION_PATTERN = Pattern.compile("^([89]|\\d{2})\\.\\d+\\.\\d+.*");

    private final boolean isOss;
    private final boolean isAtLeastMajorVersion8;
    private Optional<byte[]> caCertAsBytes = Optional.empty();

    /**
     * @deprecated use {@link ElasticsearchContainer(DockerImageName)} instead
     */
    @Deprecated
    public ElasticsearchContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * Create an Elasticsearch Container by passing the full docker image name
     * @param dockerImageName Full docker image name as a {@link String}, like: docker.elastic.co/elasticsearch/elasticsearch:7.9.2
     */
    public ElasticsearchContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * Create an Elasticsearch Container by passing the full docker image name
     * @param dockerImageName Full docker image name as a {@link DockerImageName}, like: DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.9.2")
     */
    public ElasticsearchContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, DEFAULT_OSS_IMAGE_NAME);
        this.isOss = dockerImageName.isCompatibleWith(DEFAULT_OSS_IMAGE_NAME);

        logger().info("Starting an elasticsearch container using [{}]", dockerImageName);
        withNetworkAliases("elasticsearch-" + Base58.randomString(6));
        withEnv("discovery.type", "single-node");
        addExposedPorts(ELASTICSEARCH_DEFAULT_PORT, ELASTICSEARCH_DEFAULT_TCP_PORT);
        this.isAtLeastMajorVersion8 = VERSION_PATTERN.matcher(dockerImageName.getVersionPart()).matches();
        if (isAtLeastMajorVersion8) {
            // TLS using a self signed certificate is enabled by default in version 8
            // to prevent the HttpsUrlConnection to fail with certificate errors we use
            // the log message wait strategy, as there will be a single JSON encoded message
            // marking the node as started
            setWaitStrategy(new LogMessageWaitStrategy().withRegEx(".*\"message\":\"started\".*"));
            withPassword(ELASTICSEARCH_DEFAULT_PASSWORD);
        } else {
            setWaitStrategy(new HttpWaitStrategy()
                .forPort(ELASTICSEARCH_DEFAULT_PORT)
                .forStatusCodeMatching(response -> response == HTTP_OK || response == HTTP_UNAUTHORIZED)
                .withStartupTimeout(Duration.ofMinutes(2)));
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        if (isAtLeastMajorVersion8) {
            byte[] bytes = copyFileFromContainer("/usr/share/elasticsearch/config/certs/http_ca.crt", IOUtils::toByteArray);
            if (bytes.length > 0) {
                this.caCertAsBytes = Optional.of(bytes);
            }
        }
    }

    /**
     * If this is running above Elasticsearch 8, the probably self signed CA cert will be extracted
     *
     * @return byte array optional containing the CA cert extracted from the docker container
     */
    public Optional<byte[]> caCertAsBytes() {
        return caCertAsBytes;
    }

    /**
     * A SSL context based on the self signed CA, so that using this SSL Context allows to connect to the Elasticsearch service
     * @return a customized SSL Context
     */
    public SSLContext createSslContextFromCa() {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate trustedCa = factory.generateCertificate(new ByteArrayInputStream(caCertAsBytes.get()));
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(null, null);
            trustStore.setCertificateEntry("ca", trustedCa);

            final SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmfactory.init(trustStore);
            sslContext.init(null, tmfactory.getTrustManagers(), null);
            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Define the Elasticsearch password to set. It enables security behind the scene for major version below 8.0.0.
     * It's not possible to use security with the oss image.
     * @param password  Password to set
     * @return this
     */
    public ElasticsearchContainer withPassword(String password) {
        if (isOss) {
            throw new IllegalArgumentException("You can not activate security on Elastic OSS Image. " +
                "Please switch to the default distribution");
        }
        withEnv("ELASTIC_PASSWORD", password);
        if (!isAtLeastMajorVersion8) {
            // major version 8 is secure by default and does not need this to enable authentication
            withEnv("xpack.security.enabled", "true");
        }
        return this;
    }

    public String getHttpHostAddress() {
        return getHost() + ":" + getMappedPort(ELASTICSEARCH_DEFAULT_PORT);
    }

    @Deprecated // The TransportClient will be removed in Elasticsearch 8. No need to expose this port anymore in the future.
    public InetSocketAddress getTcpHost() {
        return new InetSocketAddress(getHost(), getMappedPort(ELASTICSEARCH_DEFAULT_TCP_PORT));
    }
}
