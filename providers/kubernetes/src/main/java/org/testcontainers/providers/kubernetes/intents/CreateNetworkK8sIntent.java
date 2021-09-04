package org.testcontainers.providers.kubernetes.intents;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.testcontainers.controller.intents.CreateNetworkIntent;
import org.testcontainers.controller.intents.CreateNetworkResult;

import java.util.Map;

@Slf4j
public class CreateNetworkK8sIntent implements CreateNetworkIntent {
    @Override
    public CreateNetworkIntent withName(String name) {
        log.debug("Network not supported.");
        return this;
    }

    @Override
    public CreateNetworkIntent withCheckDuplicate(boolean checkDuplicate) {
        log.debug("Network not supported.");
        return this;
    }

    @Override
    public CreateNetworkIntent withEnableIpv6(Boolean enabledIpv6) {
        log.debug("Network not supported.");
        return this;
    }

    @Override
    public CreateNetworkIntent withDriver(String driver) {
        log.debug("Network not supported.");
        return this;
    }

    @Override
    public @Nullable Map<String, String> getLabels() {
        log.debug("Network not supported.");
        return null;
    }

    @Override
    public CreateNetworkIntent withLabels(Map<String, String> labels) {
        log.debug("Network not supported.");
        return this;
    }

    @Override
    public CreateNetworkResult perform() {
       return new CreateNetworkK8sResult();
    }
}
