package org.testcontainers.controller.intents;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.HostConfig;
import org.testcontainers.controller.model.ContainerState;
import org.testcontainers.controller.model.NetworkSettings;

import java.util.List;

public interface InspectContainerResult {

    ContainerState getState();

    NetworkSettings getNetworkSettings();

    String getName();

    ContainerConfig getConfig();

    HostConfig getHostConfig();

    List<InspectContainerResponse.Mount> getMounts();

    String getId();

    String getCreated();
}
