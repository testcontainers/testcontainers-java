package org.testcontainers.controller.intents;

public interface ConnectToNetworkIntent {


    ConnectToNetworkIntent withContainerId(String containerId);

    ConnectToNetworkIntent withNetworkId(String networkId);

    void perform();
}
