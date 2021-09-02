package org.testcontainers.providers.kubernetes.intents;

import org.testcontainers.controller.intents.CreateNetworkResult;

public class CreateNetworkK8sResult implements CreateNetworkResult {
    @Override
    public String getId() {
        return "kubernetes";
    }
}
