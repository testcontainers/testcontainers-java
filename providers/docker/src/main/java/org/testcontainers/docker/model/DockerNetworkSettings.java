package org.testcontainers.docker.model;

import org.testcontainers.controller.model.ContainerNetwork;
import org.testcontainers.controller.model.NetworkSettings;
import org.testcontainers.controller.model.Ports;

import java.util.Map;
import java.util.stream.Collectors;

public class DockerNetworkSettings implements NetworkSettings {

    private final com.github.dockerjava.api.model.NetworkSettings networkSettings;


    public DockerNetworkSettings(com.github.dockerjava.api.model.NetworkSettings networkSettings) {
        this.networkSettings = networkSettings;
    }

    @Override
    public Ports getPorts() {
        return new DockerPorts(networkSettings.getPorts());
    }

    @Override
    public Map<String, ContainerNetwork> getNetworks() {
        // TODO: Lazy?
        return networkSettings.getNetworks() .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> new DockerContainerNetwork(e.getValue())
            ));
    }

    @Override
    public String getInternalContainerIp() {
        return getNetworks().values().stream()
            .findFirst()
            .map(ContainerNetwork::getIpAddress)
            .orElseThrow(() -> new IllegalStateException("No network available to extract the internal IP from!"));
    }
}
