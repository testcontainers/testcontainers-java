package org.testcontainers.providers.kubernetes;

import io.fabric8.kubernetes.api.model.apps.ReplicaSet;

public interface ExecutionLimiter {

    void checkLimits(KubernetesContext ctx, ReplicaSet replicaSet) throws KubernetesExecutionLimitException;

}
