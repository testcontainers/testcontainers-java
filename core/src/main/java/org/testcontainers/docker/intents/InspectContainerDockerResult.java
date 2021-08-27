package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.NetworkSettings;
import org.testcontainers.controller.intents.InspectContainerResult;

import java.util.List;

public class InspectContainerDockerResult implements InspectContainerResult {

    private final InspectContainerResponse exec;

    public InspectContainerDockerResult(InspectContainerResponse exec) {
        this.exec = exec;
    }


    @Override
    public InspectContainerResponse.ContainerState getState() {
        return exec.getState();
    }

    @Override
    public NetworkSettings getNetworkSettings() {
        return exec.getNetworkSettings();
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
