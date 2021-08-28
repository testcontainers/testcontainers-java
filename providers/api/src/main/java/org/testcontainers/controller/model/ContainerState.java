package org.testcontainers.controller.model;

import com.github.dockerjava.api.command.HealthState;

public interface ContainerState {
    String getStatus();

    Boolean getRunning();

    Integer getExitCode();

    Boolean getDead();

    Boolean getOOMKilled();

    String getError();

    HealthState getHealth(); // TODO: Replace return type

    boolean getPaused();

    String getStartedAt();

    String getFinishedAt();
}
