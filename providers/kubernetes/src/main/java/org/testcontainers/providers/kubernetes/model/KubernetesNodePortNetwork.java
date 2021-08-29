package org.testcontainers.providers.kubernetes.model;

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
