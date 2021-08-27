package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.ConnectToNetworkCmd;
import org.testcontainers.controller.intents.ConnectToNetworkIntent;

public class ConnectToNetworkDockerIntent implements ConnectToNetworkIntent {

    private final ConnectToNetworkCmd connectToNetworkCmd;

    public ConnectToNetworkDockerIntent(ConnectToNetworkCmd connectToNetworkCmd) {
        this.connectToNetworkCmd = connectToNetworkCmd;
    }

    @Override
    public ConnectToNetworkIntent withContainerId(String containerId) {
        connectToNetworkCmd.withContainerId(containerId);
        return this;
    }

    @Override
    public ConnectToNetworkIntent withNetworkId(String networkId) {
        connectToNetworkCmd.withNetworkId(networkId);
        return this;
    }

    @Override
    public void perform() {
        connectToNetworkCmd.exec();
    }
}
