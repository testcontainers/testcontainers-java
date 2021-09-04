package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.CreateNetworkCmd;
import org.jetbrains.annotations.Nullable;
import org.testcontainers.controller.intents.CreateNetworkIntent;
import org.testcontainers.controller.intents.CreateNetworkResult;
import org.testcontainers.docker.intents.CreateNetworkDockerResult;

import java.util.Map;

public class CreateNetworkDockerIntent implements CreateNetworkIntent {
    private final CreateNetworkCmd networkCmd;

    public CreateNetworkDockerIntent(CreateNetworkCmd networkCmd) {
        this.networkCmd = networkCmd;
    }

    @Override
    public CreateNetworkIntent withName(String name) {
        networkCmd.withName(name);
        return this;
    }

    @Override
    public CreateNetworkIntent withCheckDuplicate(boolean checkDuplicate) {
        networkCmd.withCheckDuplicate(checkDuplicate);
        return this;
    }

    @Override
    public CreateNetworkIntent withEnableIpv6(Boolean enabledIpv6) {
        networkCmd.withEnableIpv6(enabledIpv6);
        return this;
    }

    @Override
    public CreateNetworkIntent withDriver(String driver) {
        networkCmd.withDriver(driver);
        return this;
    }

    @Override
    public @Nullable Map<String, String> getLabels() {
        return networkCmd.getLabels();
    }

    @Override
    public CreateNetworkIntent withLabels(Map<String, String> labels) {
        networkCmd.withLabels(labels);
        return this;
    }

    @Override
    public CreateNetworkResult perform() {
        return new CreateNetworkDockerResult(networkCmd.exec());
    }
}
