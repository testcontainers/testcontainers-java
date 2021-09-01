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

    @Override
    public String exposedPortsIpAddress() {
        return DockerClientFactory.instance().dockerHostIpAddress();
    }

    @Override
    public boolean supportsExecution() {
        String executionDriver = DockerClientFactory.instance().getActiveExecutionDriver();

        // Could be null starting from Docker 1.13
        return executionDriver == null || !executionDriver.startsWith("lxc");
    }

    @Override
    public boolean isFileMountingSupported() {
        return DockerClientFactory.instance().isFileMountingSupported();
    }
}
