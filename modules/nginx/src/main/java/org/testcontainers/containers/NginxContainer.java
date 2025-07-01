package org.testcontainers.containers;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.utility.DockerImageName;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

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

    /**
     * @return the ports on which to check if the container is ready
     * @deprecated use {@link #getLivenessCheckPortNumbers()} instead
     */
    @NotNull
    @Override
    @Deprecated
    protected Set<Integer> getLivenessCheckPorts() {
        return super.getLivenessCheckPorts();
    }

    public URL getBaseUrl(String scheme, int port) throws MalformedURLException {
        return new URL(scheme + "://" + getHost() + ":" + getMappedPort(port));
    }

    @Deprecated
    public void setCustomContent(String htmlContentPath) {
        addFileSystemBind(htmlContentPath, "/usr/share/nginx/html", BindMode.READ_ONLY);
    }

    @Deprecated
    public NginxContainer withCustomContent(String htmlContentPath) {
        this.setCustomContent(htmlContentPath);
        return self();
    }
}
