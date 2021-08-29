package org.testcontainers.providers.kubernetes.intents;

import org.testcontainers.controller.intents.InspectExecIntent;
import org.testcontainers.controller.intents.InspectExecResult;
import org.testcontainers.providers.kubernetes.KubernetesContext;
import org.testcontainers.providers.kubernetes.execution.KubernetesExecution;

public class InspectExecK8sIntent implements InspectExecIntent {
    private final KubernetesContext ctx;
    private final ExecCreateK8sIntent command;
    private final KubernetesExecution kubernetesExecution;

    public InspectExecK8sIntent(KubernetesContext ctx, ExecCreateK8sIntent command, KubernetesExecution kubernetesExecution) {
        this.ctx = ctx;
        this.command = command;
        this.kubernetesExecution = kubernetesExecution;
    }

    @Override
    public InspectExecResult perform() {
        return new InspectExecK8sResult(kubernetesExecution);
    }
}
