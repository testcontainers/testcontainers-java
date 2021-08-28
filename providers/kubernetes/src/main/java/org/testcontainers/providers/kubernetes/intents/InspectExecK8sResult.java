package org.testcontainers.providers.kubernetes.intents;

import org.testcontainers.controller.intents.InspectExecResult;
import org.testcontainers.providers.kubernetes.execution.KubernetesExecution;
import org.testcontainers.providers.kubernetes.execution.KubernetesExecutionListener;

public class InspectExecK8sResult implements InspectExecResult {
    private final KubernetesExecution kubernetesExecution;

    public InspectExecK8sResult(KubernetesExecution kubernetesExecution) {
        this.kubernetesExecution = kubernetesExecution;
    }

    @Override
    public Integer getExitCode() {
        KubernetesExecutionListener listener = kubernetesExecution.getListener();
        listener.waitForCompletion();
        return listener.getExitCode();
    }
}
