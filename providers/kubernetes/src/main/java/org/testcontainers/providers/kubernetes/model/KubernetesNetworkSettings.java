package org.testcontainers.providers.kubernetes.model;

import io.fabric8.kubernetes.api.model.Service;
import org.testcontainers.controller.model.ContainerNetwork;
import org.testcontainers.controller.model.NetworkSettings;
import org.testcontainers.controller.model.Ports;
import org.testcontainers.providers.kubernetes.KubernetesContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KubernetesNetworkSettings implements NetworkSettings {
    private final KubernetesContext ctx;
    private final Service service;

    public KubernetesNetworkSettings(
        KubernetesContext ctx,
        Service service
    ) {
        this.ctx = ctx;
        this.service = service;
    }

    @Override
    public Ports getPorts() {
        return new KubernetesPorts(service);
    }



    @Override
    public Map<String, ContainerNetwork> getNetworks() {
        Map<String, ContainerNetwork> result = new HashMap<>();
        if (service != null && service.getSpec().getType().equals("NodePort")) {
            Optional<String> address = ctx.getNodePortAddress();
            address.ifPresent(s -> result.put("host", new KubernetesNodePortNetwork(s)));
        }
        return result;
    }
}
