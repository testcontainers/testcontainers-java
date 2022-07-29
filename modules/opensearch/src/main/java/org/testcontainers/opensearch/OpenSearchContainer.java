package org.testcontainers.opensearch;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Optional;

/**
 * Represents an OpenSearch Docker instance which exposes by default port 9200 and 9300 (transport.tcp.port)
 * The docker image is by default fetched from opensearchproject/opensearch
 */
@Slf4j
public class OpenSearchContainer extends GenericContainer<OpenSearchContainer> {

    /**
     * OpenSearch Default Password
     */
    public static final String OPENSEARCH_DEFAULT_PASSWORD = "admin";

    /**
     * OpenSearch Default HTTP port
     */
    private static final int OPENSEARCH_DEFAULT_PORT = 9200;


    /**
     * OpenSearch Docker base image
     */
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "opensearchproject/opensearch"
    );

    /**
     * OpenSearch Default version
     */
    @Deprecated
    protected static final String DEFAULT_TAG = "2.1.0";

    private Optional<byte[]> caCertAsBytes = Optional.empty();

    private String certPath = "/usr/share/opensearch/config/root-ca.pem";

    /**
     * @deprecated use {@link OpenSearchContainer (DockerImageName)} instead
     */
    @Deprecated
    public OpenSearchContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * Create an OpenSearch Container by passing the full docker image name
     * @param dockerImageName Full docker image name as a {@link String}
     */
    public OpenSearchContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * Create an OpenSearch Container by passing the full docker image name
     * @param dockerImageName Full docker image name as a {@link DockerImageName}
     */
    public OpenSearchContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        logger().info("Starting an OpenSearch container using [{}]", dockerImageName);
        withNetworkAliases("opensearch-" + Base58.randomString(6));
        withEnv("discovery.type", "single-node");
        addExposedPorts(OPENSEARCH_DEFAULT_PORT);
        setWaitStrategy(new HttpWaitStrategy().usingTls().allowInsecure().forPath("/_plugins/_security/health").forStatusCode(200));
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        if (StringUtils.isNotEmpty(certPath)) {
            try {
                byte[] bytes = copyFileFromContainer(certPath, IOUtils::toByteArray);
                if (bytes.length > 0) {
                    this.caCertAsBytes = Optional.of(bytes);
                }
            } catch (NotFoundException e) {
                // just emit an error message, but do not throw an exception
                // this might be ok, if the docker image is accidentally looking like version 8 or latest
                // can happen if OpenSearch is repackaged, i.e. with custom plugins
                log.warn("CA cert under " + certPath + " not found.");
            }
        }
    }

    /**
     * This will return the probably self-signed CA cert that has been extracted
     *
     * @return byte array optional containing the CA cert extracted from the docker container
     */
    public Optional<byte[]> caCertAsBytes() {
        return caCertAsBytes;
    }

    /**
     * An SSL context based on the self-signed CA, so that using this SSL Context allows to connect to the OpenSearch service
     * @return a customized SSL Context
     */
    public SSLContext createSslContextFromCa() {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            if (caCertAsBytes.isPresent()) {
                Certificate trustedCa = factory.generateCertificate(new ByteArrayInputStream(caCertAsBytes.get()));
                KeyStore trustStore = KeyStore.getInstance("pkcs12");
                trustStore.load(null, null);
                trustStore.setCertificateEntry("ca", trustedCa);
                final SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
                TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmfactory.init(trustStore);
                sslContext.init(null, tmfactory.getTrustManagers(), null);
                return sslContext;
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Configure a CA cert path that is not the default
     *
     * @param certPath Path to the CA certificate within the Docker container to extract it from after start up
     * @return this
     */
    public OpenSearchContainer withCertPath(String certPath) {
        this.certPath = certPath;
        return this;
    }

    public String getHttpHostAddress() {
        return getHost() + ":" + getMappedPort(OPENSEARCH_DEFAULT_PORT);
    }
}
