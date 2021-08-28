package org.testcontainers.docker;

import org.testcontainers.controller.ContainerController;
import org.testcontainers.controller.ContainerProvider;

public class DockerContainerProvider implements ContainerProvider {

    @Override
    public ContainerController lazyController() {
        return new DockerContainerController(DockerClientFactory.lazyClient());
    }

    @Override
    public ContainerController controller() {
        return new DockerContainerController(DockerClientFactory.instance().client());
    }
}
