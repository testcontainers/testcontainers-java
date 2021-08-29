package org.testcontainers.providers.kubernetes.intents;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import org.testcontainers.controller.intents.InspectContainerIntent;
import org.testcontainers.controller.intents.InspectContainerResult;
import org.testcontainers.providers.kubernetes.KubernetesContext;

public class InspectContainerK8sIntent implements InspectContainerIntent {
    private final KubernetesContext ctx;
    private final String containerId;
    private final ReplicaSet replicaSet;

    public InspectContainerK8sIntent(
        KubernetesContext ctx,
        String containerId,
        ReplicaSet replicaSet
    ) {
        this.ctx = ctx;
        this.containerId = containerId;
        this.replicaSet = replicaSet;
    }

    @Override
    public InspectContainerResult perform() {
        Service service = ctx.getClient().services()
            .inNamespace(replicaSet.getMetadata().getNamespace())
            .withName(replicaSet.getMetadata().getName())
            .get();

        return new InspectContainerK8sResult(ctx, replicaSet, containerId, service);
    }
}
