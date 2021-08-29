package org.testcontainers.providers.kubernetes.intents;

import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import org.testcontainers.controller.intents.StartContainerIntent;
import org.testcontainers.providers.kubernetes.KubernetesContext;

public class StartContainerK8sIntent implements StartContainerIntent {

    private final KubernetesContext ctx;
    private final ReplicaSet replicaSet;

    public StartContainerK8sIntent(KubernetesContext ctx, ReplicaSet replicaSet) {
        this.ctx = ctx;
        this.replicaSet = replicaSet;
    }

    @Override
    public void perform() {
        ctx.getClient()
            .apps()
            .replicaSets()
            .inNamespace(ctx.getNamespaceProvider().getNamespace())
            .withName(replicaSet.getMetadata().getName())
            .scale(1);
    }
}
