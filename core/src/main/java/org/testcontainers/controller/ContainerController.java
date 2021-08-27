package org.testcontainers.controller;

import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.TagImageCmd;
import org.testcontainers.controller.intents.ConnectToNetworkIntent;
import org.testcontainers.controller.intents.CopyArchiveFromContainerIntent;
import org.testcontainers.controller.intents.CreateContainerIntent;
import org.testcontainers.controller.intents.InspectContainerIntent;
import org.testcontainers.controller.intents.ListContainersIntent;
import org.testcontainers.controller.intents.StartContainerIntent;
import org.testcontainers.controller.intents.WaitContainerIntent;

public interface ContainerController {

    void warmup();

    CreateContainerIntent createContainerIntent(String containerImageName);

    StartContainerIntent startContainerIntent(String containerId);

    InspectContainerIntent inspectContainerIntent(String containerId);

    ListContainersIntent listContainersIntent();

    ConnectToNetworkIntent connectToNetworkIntent();

    CopyArchiveFromContainerIntent copyArchiveFromContainerIntent(String containerId, String newRecordingFileName);

    WaitContainerIntent waitContainerIntent(String containerId);

    TagImageCmd tagImageCmd(String sourceImage, String repositoryWithImage, String tag);

    LogContainerCmd logContainerCmd(String containerId);

    void checkAndPullImage(String imageName);
}
