package org.testcontainers;

import org.testcontainers.controller.ContainerController;
import org.testcontainers.docker.DockerContainerProvider;

public class ContainerControllerFactory {

    private static final ContainerProvider provider = new DockerContainerProvider();

    private static ContainerProvider getProvider() {
        return provider;
    }

    public static ContainerController lazyController() {
        return getProvider().lazyController();
    }

}
