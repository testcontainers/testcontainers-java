package org.testcontainers;

import org.testcontainers.controller.ContainerController;
import org.testcontainers.controller.ContainerProvider;
import org.testcontainers.docker.DockerContainerProvider;

public class ContainerControllerFactory {

    private static ContainerProvider instance;

    public static ContainerController lazyController() {
        return instance().lazyController();
    }

    public synchronized static ContainerProvider instance() {
        if (instance == null) {
            instance = new DockerContainerProvider();
        }
        return instance;
    }

}
