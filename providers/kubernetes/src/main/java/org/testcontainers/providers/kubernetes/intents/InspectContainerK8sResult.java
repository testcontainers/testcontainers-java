package org.testcontainers.providers.kubernetes.intents;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.HostConfig;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import org.testcontainers.controller.intents.InspectContainerResult;
import org.testcontainers.controller.model.ContainerState;
import org.testcontainers.controller.model.NetworkSettings;
import org.testcontainers.providers.kubernetes.KubernetesContext;
import org.testcontainers.providers.kubernetes.model.KubernetesContainerState;
import org.testcontainers.providers.kubernetes.model.KubernetesNetworkSettings;

import java.util.List;

public class InspectContainerK8sResult implements InspectContainerResult {

    private final KubernetesContext ctx;
    private final ReplicaSet replicaSet;
    private final String containerId;
    private final Service service;

    public InspectContainerK8sResult(
        KubernetesContext ctx,
        ReplicaSet replicaSet,
        String containerId,
        Service service
    ) {
        this.ctx = ctx;
        this.replicaSet = replicaSet;
        this.containerId = containerId;
        this.service = service;
    }


    @Override
    public ContainerState getState() {
        return new KubernetesContainerState(ctx, replicaSet, containerId);
    }

    @Override
    public NetworkSettings getNetworkSettings() {
        return new KubernetesNetworkSettings(ctx, containerId, service);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public ContainerConfig getConfig() {
        return null;
    }

    @Override
    public HostConfig getHostConfig() {
        return null;
    }

    @Override
    public List<InspectContainerResponse.Mount> getMounts() {
        return null;
    }

    @Override
    public String getId() {
        return replicaSet.getMetadata().getUid();
    }

    @Override
    public String getCreated() {
        return replicaSet.getMetadata().getCreationTimestamp();
    }
}
