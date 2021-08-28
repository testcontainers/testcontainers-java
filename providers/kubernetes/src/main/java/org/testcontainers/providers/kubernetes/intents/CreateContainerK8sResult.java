package org.testcontainers.providers.kubernetes.intents;

import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import org.testcontainers.controller.intents.CreateContainerResult;
import org.testcontainers.providers.kubernetes.KubernetesContext;

public class CreateContainerK8sResult implements CreateContainerResult {
    private final KubernetesContext ctx;
    private final ReplicaSet replicaSet;

    public CreateContainerK8sResult(KubernetesContext ctx, ReplicaSet replicaSet) {
        this.ctx = ctx;
        this.replicaSet = replicaSet;
    }

    @Override
    public String getId() {
        return replicaSet.getMetadata().getUid();
    }
}
