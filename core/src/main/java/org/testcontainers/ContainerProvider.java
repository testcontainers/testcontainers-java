package org.testcontainers;

import org.testcontainers.controller.ContainerController;

public interface ContainerProvider {

    ContainerController lazyController();
}
