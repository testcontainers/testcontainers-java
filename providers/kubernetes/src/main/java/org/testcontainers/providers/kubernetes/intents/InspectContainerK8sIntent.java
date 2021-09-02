package org.testcontainers.providers.kubernetes.intents;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import org.testcontainers.controller.intents.InspectContainerIntent;
import org.testcontainers.controller.intents.InspectContainerResult;
import org.testcontainers.providers.kubernetes.KubernetesContext;
import org.testcontainers.providers.kubernetes.networking.NetworkStrategy;

public class InspectContainerK8sIntent implements InspectContainerIntent {
    private final KubernetesContext ctx;
    private final NetworkStrategy networkStrategy;
    private final String containerId;
    private final ReplicaSet replicaSet;

    public InspectContainerK8sIntent(
        KubernetesContext ctx,
        NetworkStrategy networkStrategy,
        String containerId,
        ReplicaSet replicaSet
    ) {
        this.ctx = ctx;
        this.networkStrategy = networkStrategy;
        this.containerId = containerId;
        this.replicaSet = replicaSet;
    }

    @Override
    public InspectContainerResult perform() {
        Service service = networkStrategy.find(ctx, replicaSet.getMetadata().getNamespace(), replicaSet.getMetadata().getName());

        return new InspectContainerK8sResult(ctx, replicaSet, containerId, service);
    }
}
