package org.rnorth.testcontainers.containers;

import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import org.rnorth.testcontainers.containers.traits.LinkableContainer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author richardnorth
 */
public class NginxContainer extends AbstractContainer implements LinkableContainer {
    private String nginxPort;
    private String htmlContentPath;
    private Map<String, List<PortBinding>> ports;
    private List<String> binds = new ArrayList<>();

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
        return ContainerConfig.builder()
                            .image(getDockerImageName())
                            .exposedPorts("80")
                            .cmd("nginx", "-g", "daemon off;")
                            .build();
    }

    @Override
    protected void customizeHostConfigBuilder(HostConfig.Builder hostConfigBuilder) {
        hostConfigBuilder.binds(binds);
    }

    @Override
    protected String getDockerImageName() {
        return "nginx:1.7.11";
    }

    public URL getBaseUrl(String scheme, int port) throws MalformedURLException {
        return new URL(scheme + "://" + dockerHostIpAddress + ":" + ports.get(port + "/tcp").get(0).hostPort());
    }

    public void setCustomConfig(String htmlContentPath) {
        binds.add(htmlContentPath + ":/usr/share/nginx/html:ro");
    }

    @Override
    public String getContainerId() {
        return containerId;
    }
}
