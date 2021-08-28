package org.testcontainers.controller;

import org.testcontainers.controller.ContainerController;

public interface ContainerProvider {

    ContainerController lazyController();
    ContainerController controller();

}
