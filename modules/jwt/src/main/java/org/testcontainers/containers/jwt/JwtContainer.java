package org.testcontainers.containers.jwt;

import lombok.NonNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This container serve an Jwt public key
 */
public class JwtContainer extends GenericContainer<JwtContainer> {
    private static final int NGINX_DEFAULT_PORT = 80;
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("nginx");
    private static final String DEFAULT_TAG = "1.9.4";

    public JwtContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public JwtContainer(@NonNull final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public JwtContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    }

    @Override
    protected void configure() {
        super.configure();

        MountableFile mountableFile = MountableFile.forClasspathResource("/public");
        withCopyFileToContainer(mountableFile, "/usr/share/nginx/html");

        addExposedPort(NGINX_DEFAULT_PORT);
        setCommand("nginx", "-g", "daemon off;");
    }

    public URL issuer() {
        try {
            return new URL(host() + "/jwks.json");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public URL host() {
        try {
            return new URL(baseUrl("http", 80).toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public URL baseUrl(String scheme, int port) throws MalformedURLException {
        return new URL(scheme + "://" + getHost() + ":" + getMappedPort(port));
    }

    public TokenForgery forgery() {
        return new TokenForgery(issuer().toString());
    }
}
