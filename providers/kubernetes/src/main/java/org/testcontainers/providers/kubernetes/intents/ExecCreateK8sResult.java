package org.testcontainers.providers.kubernetes.intents;

import org.testcontainers.controller.intents.ExecCreateResult;

public class ExecCreateK8sResult implements ExecCreateResult {
    private final String uuid;
    private final ExecCreateK8sIntent execCreateK8sIntent;

    public ExecCreateK8sResult(String uuid, ExecCreateK8sIntent execCreateK8sIntent) {
        this.uuid = uuid;
        this.execCreateK8sIntent = execCreateK8sIntent;
    }

    @Override
    public String getId() {
        return uuid;
    }
}
