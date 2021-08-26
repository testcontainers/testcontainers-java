package org.testcontainers.controller;

public interface ConnectToNetworkIntent {


    ConnectToNetworkIntent withContainerId(String containerId);

    ConnectToNetworkIntent withNetworkId(String networkId);

    void perform();
}
