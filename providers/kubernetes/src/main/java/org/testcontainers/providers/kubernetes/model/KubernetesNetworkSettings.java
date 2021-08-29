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

    private final KubernetesPodIPNetwork podIpNetwork;

    public KubernetesNetworkSettings(
        KubernetesContext ctx,
        String containerId,
        Service service
    ) {
        this.ctx = ctx;
        this.service = service;
        this.podIpNetwork = new KubernetesPodIPNetwork(ctx, containerId);
    }

    @Override
    public Ports getPorts() {
        return new KubernetesPorts(service);
    }



    @Override
    public Map<String, ContainerNetwork> getNetworks() {
        Map<String, ContainerNetwork> result = new HashMap<>();
        result.put(KubernetesPodIPNetwork.ID, podIpNetwork);
        if(service.getSpec().getClusterIP() != null) {
            result.put(KubernetesClusterIPNetwork.ID, new KubernetesClusterIPNetwork(service));
        }
        if (service != null && service.getSpec().getType().equals("NodePort")) {
            Optional<String> address = ctx.getNodePortAddress();
            address.ifPresent(s -> result.put("host", new KubernetesNodePortNetwork(s)));
        }
        return result;
    }

    @Override
    public String getInternalContainerIp() {
        return podIpNetwork.getIpAddress();
    }
}
