package org.testcontainers.providers.kubernetes.intents;

import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import org.testcontainers.controller.intents.KillContainerIntent;
import org.testcontainers.providers.kubernetes.KubernetesContext;

public class KillContainerK8sIntent implements KillContainerIntent {
    private final KubernetesContext ctx;
    private final ReplicaSet replicaSet;

    public KillContainerK8sIntent(KubernetesContext ctx, ReplicaSet replicaSet) {
        this.ctx = ctx;
        this.replicaSet = replicaSet;
    }

    @Override
    public void perform() {
        ctx.getClient()
            .apps()
            .replicaSets()
            .inNamespace(replicaSet.getMetadata().getNamespace())
            .withName(replicaSet.getMetadata().getName())
            .scale(0);
    }
}
