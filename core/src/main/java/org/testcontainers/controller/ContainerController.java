package org.testcontainers.controller;

import com.github.dockerjava.api.command.AsyncDockerCmd;
import com.github.dockerjava.api.command.AttachContainerCmd;
import com.github.dockerjava.api.command.ConnectToNetworkCmd;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.EventsCmd;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.model.AuthConfig;
import org.testcontainers.lifecycle.Startable;

public interface ContainerController {

    void warmup();

    StartContainerCmd startContainerCmd(String containerId);

    InspectContainerCmd inspectContainerCmd(String containerId);

    ListContainersCmd listContainersCmd();

    ConnectToNetworkIntent connectToNetworkIntent();

    CreateContainerCmd createContainerCmd(String dockerImageName);

    CopyArchiveFromContainerCmd copyArchiveFromContainerCmd(String containerId, String newRecordingFileName);

    WaitContainerCmd waitContainerCmd(String containerId);
}
