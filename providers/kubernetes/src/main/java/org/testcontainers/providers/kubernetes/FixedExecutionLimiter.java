package org.testcontainers.providers.kubernetes;

import io.fabric8.kubernetes.api.model.apps.ReplicaSet;

public class FixedExecutionLimiter implements ExecutionLimiter {

    private final int maxReplicaSets;

    private static final FixedExecutionLimiter DEFAULT = new FixedExecutionLimiter(20);

    public FixedExecutionLimiter(
        int maxReplicaSets
    ) {
        this.maxReplicaSets = maxReplicaSets;
    }

    public static ExecutionLimiter defaultLimiter() {
        return DEFAULT;
    }


    @Override
    public void checkLimits(KubernetesContext ctx, ReplicaSet replicaSet) throws KubernetesExecutionLimitException {
        int current = ctx.getClient()
            .apps()
            .replicaSets()
            .inNamespace(replicaSet.getMetadata().getNamespace())
            .list()
            .getItems()
            .size();
        if(current >= maxReplicaSets) {
            throw new KubernetesExecutionLimitException(
                String.format("There are currently %d of %d allowed ReplicaSets.", current, maxReplicaSets)
            );
        }
    }
}
