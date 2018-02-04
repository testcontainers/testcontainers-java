package org.testcontainers.test;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.GenericContainer;

class Container extends GenericContainer<Container> {

    Container() {
        super("alpine:3.7");
        setCommand("sh", "-c", "while true; do echo something; sleep 1; done");
    }

    String ipAddress() {
        final InspectContainerResponse inspected = getDockerClient().inspectContainerCmd(getContainerId()).exec();
        return inspected.getNetworkSettings().getNetworks().get("bridge").getIpAddress();
    }

    Container renameTo(String newName) {
        getDockerClient().renameContainerCmd(containerId).withName(newName).exec();
        return this;
    }
}
