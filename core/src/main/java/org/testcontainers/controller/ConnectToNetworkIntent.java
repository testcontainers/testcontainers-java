package org.testcontainers.controller;

import com.github.dockerjava.api.command.AsyncDockerCmd;
import com.github.dockerjava.api.command.ConnectToNetworkCmd;

public interface ConnectToNetworkIntent {


    ConnectToNetworkIntent withContainerId(String containerId);

    ConnectToNetworkIntent withNetworkId(String networkId);

    void perform();
}
