package org.testcontainers.providers.kubernetes.intents;

import org.testcontainers.controller.intents.ExecCreateIntent;
import org.testcontainers.controller.intents.ExecCreateResult;
import org.testcontainers.providers.kubernetes.KubernetesContext;

public class ExecCreateK8sIntent implements ExecCreateIntent {
    private final KubernetesContext ctx;
    private final String containerId;

    private boolean attachStdout = false;
    private boolean attachStderr;
    private String[] command = new String[]{};

    public ExecCreateK8sIntent(KubernetesContext ctx, String containerId) {
        this.ctx = ctx;
        this.containerId = containerId;
    }

    public boolean isAttachStdout() {
        return attachStdout;
    }

    public boolean isAttachStderr() {
        return attachStderr;
    }

    public String[] getCommand() {
        return command;
    }

    public String getContainerId() {
        return containerId;
    }

    @Override
    public ExecCreateIntent withAttachStdout(boolean attachStdout) {
        this.attachStdout = attachStdout;
        return this;
    }

    @Override
    public ExecCreateIntent withAttachStderr(boolean attachStderr) {
        this.attachStderr = attachStderr;
        return this;
    }

    @Override
    public ExecCreateIntent withCmd(String... command) {
        this.command = command;
        return this;
    }

    @Override
    public ExecCreateResult perform() {
        return ctx.registerCommand(this);
    }
}
