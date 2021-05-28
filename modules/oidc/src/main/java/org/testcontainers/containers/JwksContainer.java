package org.testcontainers.containers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


/**
 * TODO add docs
 */
public class JwksContainer extends GenericContainer<JwksContainer> {

    private static final int DEFAULT_PORT = 8080;
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("raynigon/minimal-http");
    private static final String DEFAULT_TAG = "0.0.1";

    public JwksContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public JwksContainer(DockerImageName image) {
        super(image);
        image.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    }

    protected void configure() {
        super.configure();
        copyResources();
        setWaitStrategy(new HttpWaitStrategy().forPort(DEFAULT_PORT).forPath("/jwks.json"));
        addExposedPort(DEFAULT_PORT);
    }

    public void start() {
        super.start();
        createOpenidConfiguration();
    }

    public final URL host() {
        try {
            return new URL(baseUrl("http", DEFAULT_PORT).toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public final String issuer() {
        return "" + host() + '/';
    }

    public final URL baseUrl() throws MalformedURLException {
        return baseUrl("http", DEFAULT_PORT);
    }

    public final URL baseUrl(String scheme) throws MalformedURLException {
        return baseUrl(scheme, DEFAULT_PORT);
    }

    public final URL baseUrl(int port) throws MalformedURLException {
        return baseUrl("http", port);
    }

    public final URL baseUrl(String scheme, int port) throws MalformedURLException {
        return new URL(scheme + "://" + getHost() + ":" + getMappedPort(port));
    }

    public final TokenForgery forgery() {
        return new TokenForgery(issuer());
    }

    private void copyResources() {
        MountableFile file = MountableFile.forClasspathResource("/org/testcontainers/jwks/public/jwks.json");
        withCopyFileToContainer(file, "/data/jwks.json");
    }

    @SneakyThrows
    private void createOpenidConfiguration() {
        Map<String, Object> openidConfig = new HashMap<>();
        openidConfig.put("issuer", "" + host() + '/');
        openidConfig.put("jwks_uri", host() + "/jwks.json");
        String content = (new ObjectMapper()).writeValueAsString(openidConfig);
        execAction("/cli", "mkdir", "/data/.well-known/");
        execAction("/cli", "copyTo", "/data/.well-known/openid-configuration", content);
    }

    @SneakyThrows
    private void execAction(String... command) {
        ExecResult result = execInContainer(command);
        if (result.getExitCode() != 0) {
            throw new JwtContainerConfigurationException(command[0], result);
        }
    }
}

