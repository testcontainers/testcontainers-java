package org.testcontainers.docker.model;

import com.github.dockerjava.api.command.HealthState;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.controller.model.ContainerState;

public class DockerContainerState implements ContainerState {
    private final InspectContainerResponse.ContainerState state;

    public DockerContainerState(InspectContainerResponse.ContainerState state) {
        this.state = state;
    }

    @Override
    public String getStatus() {
        return state.getStatus();
    }

    @Override
    public Boolean getRunning() {
        return state.getRunning();
    }

    @Override
    public Integer getExitCode() {
        return state.getExitCode();
    }

    @Override
    public Boolean getDead() {
        return state.getDead();
    }

    @Override
    public Boolean getOOMKilled() {
        return state.getOOMKilled();
    }

    @Override
    public String getError() {
        return state.getError();
    }

    @Override
    public HealthState getHealth() {
        return state.getHealth();
    }

    @Override
    public boolean getPaused() {
        return state.getPaused();
    }

    @Override
    public String getStartedAt() {
        return state.getStartedAt();
    }

    @Override
    public String getFinishedAt() {
        return state.getFinishedAt();
    }
}
