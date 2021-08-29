package org.testcontainers.providers.kubernetes.model;

import org.testcontainers.controller.model.ContainerNetwork;
import org.testcontainers.providers.kubernetes.KubernetesContext;

public class KubernetesPodIPNetwork implements ContainerNetwork {

    public static String ID = "pod";
    private final KubernetesContext ctx;
    private final String containerId;

    public KubernetesPodIPNetwork(KubernetesContext ctx, String containerId) {
        this.ctx = ctx;
        this.containerId = containerId;
    }

    @Override
    public String getIpAddress() {
        return ctx.findPodForContainerId(containerId).getStatus().getPodIP();
    }

    @Override
    public String getNetworkID() {
        return ID;
    }
}
