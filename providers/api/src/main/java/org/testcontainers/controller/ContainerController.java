package org.testcontainers.controller;

import com.github.dockerjava.api.command.AsyncDockerCmd;
import com.github.dockerjava.api.command.CommitCmd;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import org.testcontainers.controller.intents.ConnectToNetworkIntent;
import org.testcontainers.controller.intents.CopyArchiveFromContainerIntent;
import org.testcontainers.controller.intents.CopyArchiveToContainerIntent;
import org.testcontainers.controller.intents.CreateContainerIntent;
import org.testcontainers.controller.intents.ExecCreateIntent;
import org.testcontainers.controller.intents.ExecStartIntent;
import org.testcontainers.controller.intents.InspectContainerIntent;
import org.testcontainers.controller.intents.InspectExecIntent;
import org.testcontainers.controller.intents.InspectImageIntent;
import org.testcontainers.controller.intents.KillContainerIntent;
import org.testcontainers.controller.intents.ListContainersIntent;
import org.testcontainers.controller.intents.ListImagesIntent;
import org.testcontainers.controller.intents.ListNetworksIntent;
import org.testcontainers.controller.intents.LogContainerIntent;
import org.testcontainers.controller.intents.PullImageIntent;
import org.testcontainers.controller.intents.RemoveContainerIntent;
import org.testcontainers.controller.intents.RemoveImageIntent;
import org.testcontainers.controller.intents.RemoveNetworkIntent;
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

    InspectImageIntent inspectImageIntent(String asCanonicalNameString) throws UnsupportedProviderOperationException;

    ListImagesIntent listImagesIntent() throws UnsupportedProviderOperationException;

    PullImageIntent pullImageIntent(String repository) throws UnsupportedProviderOperationException;

    KillContainerIntent killContainerIntent(String containerId);

    RemoveContainerIntent removeContainerIntent(String containerId);

    ListNetworksIntent listNetworksIntent();

    RemoveNetworkIntent removeNetworkIntent(String id);

    RemoveImageIntent removeImageIntent(String imageReference);

    ExecCreateIntent execCreateCmd(String containerId); // TODO: Rename

    ExecStartIntent execStartCmd(String commandId); // TODO: Rename

    InspectExecIntent inspectExecCmd(String commandId); // TODO: Rename

    CopyArchiveToContainerIntent copyArchiveToContainerIntent(String containerId);
}
