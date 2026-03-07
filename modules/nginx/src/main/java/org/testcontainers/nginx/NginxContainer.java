package org.testcontainers.nginx;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.net.MalformedURLException;
import java.net.URL;

public class NginxContainer extends GenericContainer<NginxContainer> {

    private static final int NGINX_DEFAULT_PORT = 80;

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("nginx");

    public NginxContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public NginxContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        addExposedPort(NGINX_DEFAULT_PORT);
        setCommand("nginx", "-g", "daemon off;");
    }

    public URL getBaseUrl(String scheme, int port) throws MalformedURLException {
        return new URL(scheme + "://" + getHost() + ":" + getMappedPort(port));
    }

    public URL getBaseUrl(String scheme) throws MalformedURLException {
        return getBaseUrl(scheme, NGINX_DEFAULT_PORT);
    }
}
