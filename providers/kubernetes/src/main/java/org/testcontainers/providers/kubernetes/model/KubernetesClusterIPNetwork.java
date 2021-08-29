package org.testcontainers.providers.kubernetes.model;

import io.fabric8.kubernetes.api.model.Service;
import org.testcontainers.controller.model.ContainerNetwork;

public class KubernetesClusterIPNetwork implements ContainerNetwork {
    private final Service service;
    public static String ID = "svc";

    public KubernetesClusterIPNetwork(Service service) {
        this.service = service;
    }

    @Override
    public String getIpAddress() {
        return service.getSpec().getClusterIP();
    }

    @Override
    public String getNetworkID() {
        return ID;
    }
}
