package org.testcontainers.controller;

import org.testcontainers.controller.ContainerController;

public interface ContainerProvider {

    ContainerController lazyController();
    ContainerController controller();
    String exposedPortsIpAddress();

    boolean supportsExecution();

    boolean isFileMountingSupported();
}
