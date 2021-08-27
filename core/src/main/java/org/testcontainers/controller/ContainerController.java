package org.testcontainers.controller;

import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.TagImageCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import org.testcontainers.controller.intents.ConnectToNetworkIntent;
import org.testcontainers.controller.intents.CreateContainerIntent;
import org.testcontainers.controller.intents.InspectContainerIntent;
import org.testcontainers.controller.intents.ListContainersIntent;
import org.testcontainers.controller.intents.StartContainerIntent;

public interface ContainerController {

    void warmup();

    CreateContainerIntent createContainerIntent(String containerImageName);

    StartContainerIntent startContainerIntent(String containerId);

    InspectContainerIntent inspectContainerIntent(String containerId);

    ListContainersIntent listContainersIntent();

    ConnectToNetworkIntent connectToNetworkIntent();

    CopyArchiveFromContainerCmd copyArchiveFromContainerCmd(String containerId, String newRecordingFileName);

    WaitContainerCmd waitContainerCmd(String containerId);

    TagImageCmd tagImageCmd(String sourceImage, String repositoryWithImage, String tag);

    LogContainerCmd logContainerCmd(String containerId);

    void checkAndPullImage(String imageName);
}
