package org.testcontainers.controller;

import org.testcontainers.controller.intents.ConnectToNetworkIntent;
import org.testcontainers.controller.intents.CopyArchiveFromContainerIntent;
import org.testcontainers.controller.intents.CreateContainerIntent;
import org.testcontainers.controller.intents.InspectContainerIntent;
import org.testcontainers.controller.intents.ListContainersIntent;
import org.testcontainers.controller.intents.LogContainerIntent;
import org.testcontainers.controller.intents.StartContainerIntent;
import org.testcontainers.controller.intents.TagImageIntent;
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

    TagImageIntent tagImageIntent(String sourceImage, String repositoryWithImage, String tag);

    LogContainerIntent logContainerIntent(String containerId);

    void checkAndPullImage(String imageName);
}
