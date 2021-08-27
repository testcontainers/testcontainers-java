package org.testcontainers.controller;

import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;

public interface ContainerController {

    void warmup();

    CreateContainerIntent createContainerIntent(String containerImageName);

    StartContainerIntent startContainerIntent(String containerId);

    InspectContainerCmd inspectContainerCmd(String containerId);

    ListContainersCmd listContainersCmd();

    ConnectToNetworkIntent connectToNetworkIntent();

    CopyArchiveFromContainerCmd copyArchiveFromContainerCmd(String containerId, String newRecordingFileName);

    WaitContainerCmd waitContainerCmd(String containerId);
}
