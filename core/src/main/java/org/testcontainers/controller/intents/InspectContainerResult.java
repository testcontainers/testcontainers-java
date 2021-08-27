package org.testcontainers.controller.intents;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.NetworkSettings;

import java.util.List;

public interface InspectContainerResult {

    InspectContainerResponse.ContainerState getState();

    NetworkSettings getNetworkSettings();

    String getName();

    ContainerConfig getConfig();

    HostConfig getHostConfig();

    List<InspectContainerResponse.Mount> getMounts();

    String getId();

    String getCreated();
}
