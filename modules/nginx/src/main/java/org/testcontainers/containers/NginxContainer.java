package org.testcontainers.containers;

import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.PortBinding;
import org.testcontainers.containers.traits.LinkableContainer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author richardnorth
 */
public class NginxContainer extends GenericContainer implements LinkableContainer {
    private String nginxPort;
    private Map<String, List<PortBinding>> ports;
    private List<String> binds = new ArrayList<>();
    private String[] exposedPorts;

    public NginxContainer() {
        super("nginx:1.9.4");
    }

    @Override
    protected void containerIsStarting(ContainerInfo containerInfo) {
        ports = containerInfo.networkSettings().ports();
        nginxPort = ports.get("80/tcp").get(0).hostPort();
    }

    @Override
    protected String getLivenessCheckPort() {
        return nginxPort;
    }

    @Override
    protected ContainerConfig getContainerConfig() {
        withImageName(getDockerImageName());
        withExposedPorts(80);
        withCommand("nginx", "-g", "daemon off;");

        return super.getContainerConfig();
    }

    @Override
    protected String getDockerImageName() {
        return "nginx:1.7.11";
    }

    public URL getBaseUrl(String scheme, int port) throws MalformedURLException {
        return new URL(scheme + "://" + getIpAddress() + ":" + ports.get(port + "/tcp").get(0).hostPort());
    }

    public void setCustomConfig(String htmlContentPath) {
        binds.add(htmlContentPath + ":/usr/share/nginx/html:ro");
    }

    public void setExposedPorts(String[] ports) {
        this.exposedPorts = ports;
    }

    public NginxContainer withCustomContent(String htmlContentPath) {
        this.setCustomConfig(htmlContentPath);
        return this;
    }

    public NginxContainer withExposedPorts(String... ports) {
        this.setExposedPorts(ports);
        return this;
    }
}
