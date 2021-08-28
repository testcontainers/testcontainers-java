package org.testcontainers.providers.kubernetes.execution;

import io.fabric8.kubernetes.client.dsl.ExecWatch;

public class KubernetesExecution {
    private final KubernetesExecutionListener listener;
    private final ExecWatch execWatch;

    public KubernetesExecution(KubernetesExecutionListener listener, ExecWatch execWatch) {
        this.listener = listener;
        this.execWatch = execWatch;
    }

    public ExecWatch getExecWatch() {
        return execWatch;
    }

    public KubernetesExecutionListener getListener() {
        return listener;
    }
}
