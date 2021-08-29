package org.testcontainers.controller.model;


import java.util.Map;

public interface NetworkSettings {
    Ports getPorts();

    Map<String, ContainerNetwork> getNetworks();

    String getInternalContainerIp();
}
