package org.testcontainers.elasticsearch;

import com.github.dockerjava.api.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Testcontainers implementation for Elasticsearch.
 * <p>
 * Supported image: {@code docker.elastic.co/elasticsearch/elasticsearch}, {@code elasticsearch}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>HTTP: 9200</li>
 *     <li>TCP Transport: 9300</li>
 * </ul>
 */
@Slf4j
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
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "docker.elastic.co/elasticsearch/elasticsearch"
    );

    @Deprecated
    private static final DockerImageName DEFAULT_OSS_IMAGE_NAME = DockerImageName.parse(
        "docker.elastic.co/elasticsearch/elasticsearch-oss"
    );

    private static final DockerImageName ELASTICSEARCH_IMAGE_NAME = DockerImageName.parse("elasticsearch");

    /**
     * Elasticsearch Default version
     */
    @Deprecated
    protected static final String DEFAULT_TAG = "7.9.2";

    @Deprecated
    private boolean isOss = false;

    private final boolean isAtLeastMajorVersion8;

    private String certPath = "/usr/share/elasticsearch/config/certs/http_ca.crt";

    /**
     * @deprecated use {@link #ElasticsearchContainer(DockerImageName)} instead
     */
    @Deprecated
    public ElasticsearchContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * Create an Elasticsearch Container by passing the full docker image name
     *
     * @param dockerImageName Full docker image name as a {@link String}, like: docker.elastic.co/elasticsearch/elasticsearch:7.9.2
     */
    public ElasticsearchContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * Create an Elasticsearch Container by passing the full docker image name
     *
     * @param dockerImageName Full docker image name as a {@link DockerImageName}, like: DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.9.2")
     */
    public ElasticsearchContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, DEFAULT_OSS_IMAGE_NAME, ELASTICSEARCH_IMAGE_NAME);

        if (dockerImageName.isCompatibleWith(DEFAULT_OSS_IMAGE_NAME)) {
            this.isOss = true;
            log.warn(
                "{} is not supported anymore after 7.10.2. Please switch to {}",
                dockerImageName.getUnversionedPart(),
                DEFAULT_IMAGE_NAME.getUnversionedPart()
            );
        }

        withEnv("discovery.type", "single-node");
        // disable disk threshold checks
        withEnv("cluster.routing.allocation.disk.threshold_enabled", "false");
        // Sets default memory of elasticsearch instance to 2GB
        // Spaces are deliberate to allow user to define additional jvm options as elasticsearch resolves option files lexicographically
        withClasspathResourceMapping(
            "elasticsearch-default-memory-vm.options",
            "/usr/share/elasticsearch/config/jvm.options.d/ elasticsearch-default-memory-vm.options",
            BindMode.READ_ONLY
        );
        addExposedPorts(ELASTICSEARCH_DEFAULT_PORT, ELASTICSEARCH_DEFAULT_TCP_PORT);
        this.isAtLeastMajorVersion8 =
            new ComparableVersion(dockerImageName.getVersionPart()).isGreaterThanOrEqualTo("8.0.0");
        // regex that
        //   matches 8.3 JSON logging with started message and some follow up content within the message field
        //   matches 8.0 JSON logging with no whitespace between message field and content
        //   matches 7.x JSON logging with whitespace between message field and content
        //   matches 6.x text logging with node name in brackets and just a 'started' message till the end of the line
        String regex = ".*(\"message\":\\s?\"started[\\s?|\"].*|] started\n$)";
        setWaitStrategy(new LogMessageWaitStrategy().withRegEx(regex));
        if (isAtLeastMajorVersion8) {
            withPassword(ELASTICSEARCH_DEFAULT_PASSWORD);
        }
    }

    /**
     * If this is running above Elasticsearch 8, this will return the probably self-signed CA cert that has been extracted
     *
     * @return byte array optional containing the CA cert extracted from the docker container
     */
    public Optional<byte[]> caCertAsBytes() {
        if (StringUtils.isBlank(certPath)) {
            return Optional.empty();
        }
        try {
            byte[] bytes = copyFileFromContainer(certPath, IOUtils::toByteArray);
            if (bytes.length > 0) {
                return Optional.of(bytes);
            }
        } catch (NotFoundException e) {
            // just emit an error message, but do not throw an exception
            // this might be ok, if the docker image is accidentally looking like version 8 or latest
            // can happen if Elasticsearch is repackaged, i.e. with custom plugins
            log.warn("CA cert under " + certPath + " not found.");
        }
        return Optional.empty();
    }

    /**
     * A SSL context based on the self-signed CA, so that using this SSL Context allows to connect to the Elasticsearch service
     * @return a customized SSL Context
     */
    public SSLContext createSslContextFromCa() {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate trustedCa = factory.generateCertificate(
                new ByteArrayInputStream(
                    caCertAsBytes()
                        .orElseThrow(() -> new IllegalStateException("CA cert under " + certPath + " not found."))
                )
            );
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
     * @param password Password to set
     * @return this
     */
    public ElasticsearchContainer withPassword(String password) {
        if (isOss) {
            throw new IllegalArgumentException(
                "You can not activate security on Elastic OSS Image. Please switch to the default distribution"
            );
        }
        withEnv("ELASTIC_PASSWORD", password);
        if (!isAtLeastMajorVersion8) {
            // major version 8 is secure by default and does not need this to enable authentication
            withEnv("xpack.security.enabled", "true");
        }
        return this;
    }

    /**
     * Configure a CA cert path that is not the default
     *
     * @param certPath Path to the CA certificate within the Docker container to extract it from after start up
     * @return this
     */
    public ElasticsearchContainer withCertPath(String certPath) {
        this.certPath = certPath;
        return this;
    }

    public String getHttpHostAddress() {
        return getHost() + ":" + getMappedPort(ELASTICSEARCH_DEFAULT_PORT);
    }

    @Deprecated
    // The TransportClient will be removed in Elasticsearch 8. No need to expose this port anymore in the future.
    public InetSocketAddress getTcpHost() {
        return new InetSocketAddress(getHost(), getMappedPort(ELASTICSEARCH_DEFAULT_TCP_PORT));
    }
}
