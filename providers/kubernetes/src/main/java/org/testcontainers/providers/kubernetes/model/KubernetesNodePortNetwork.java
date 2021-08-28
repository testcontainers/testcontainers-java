package org.testcontainers.providers.kubernetes.model;

import io.fabric8.kubernetes.api.model.Service;
import org.testcontainers.controller.model.ContainerNetwork;

public class KubernetesNodePortNetwork implements ContainerNetwork {
    private final String ipAddress;

    public KubernetesNodePortNetwork(String ipAddress) {
        this.ipAddress = ipAddress;
    }


    @Override
    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public String getNetworkID() {
        return "host";
    }
}
