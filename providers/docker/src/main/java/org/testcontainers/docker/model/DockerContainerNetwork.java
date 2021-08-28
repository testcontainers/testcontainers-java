package org.testcontainers.docker.model;


import org.testcontainers.controller.model.ContainerNetwork;

public class DockerContainerNetwork implements ContainerNetwork {
    private final com.github.dockerjava.api.model.ContainerNetwork containerNetwork;

    public DockerContainerNetwork(com.github.dockerjava.api.model.ContainerNetwork containerNetwork) {
        this.containerNetwork = containerNetwork;
    }


    @Override
    public String getIpAddress() {
        return containerNetwork.getIpAddress();
    }

    @Override
    public String getNetworkID() {
        return containerNetwork.getNetworkID();
    }
}
