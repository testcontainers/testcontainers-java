package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.HostConfig;
import org.testcontainers.controller.intents.InspectContainerResult;
import org.testcontainers.controller.model.ContainerState;
import org.testcontainers.controller.model.NetworkSettings;
import org.testcontainers.docker.model.DockerContainerState;
import org.testcontainers.docker.model.DockerNetworkSettings;

import java.util.List;

public class InspectContainerDockerResult implements InspectContainerResult {

    private final InspectContainerResponse exec;

    public InspectContainerDockerResult(InspectContainerResponse exec) {
        this.exec = exec;
    }


    @Override
    public ContainerState getState() {
        return new DockerContainerState(exec.getState());
    }

    @Override
    public NetworkSettings getNetworkSettings() {
        return new DockerNetworkSettings(exec.getNetworkSettings());
    }

    @Override
    public String getName() {
        return exec.getName();
    }

    @Override
    public ContainerConfig getConfig() {
        return exec.getConfig();
    }

    @Override
    public HostConfig getHostConfig() {
        return exec.getHostConfig();
    }

    @Override
    public List<InspectContainerResponse.Mount> getMounts() {
        return exec.getMounts();
    }

    @Override
    public String getId() {
        return exec.getId();
    }

    @Override
    public String getCreated() {
        return exec.getCreated();
    }
}
