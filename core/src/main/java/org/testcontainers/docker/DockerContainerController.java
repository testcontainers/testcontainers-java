package org.testcontainers.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ConnectToNetworkCmd;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.model.AuthConfig;
import org.testcontainers.controller.ContainerController;

public class DockerContainerController implements ContainerController {


    private final DockerClient dockerClient;

    public DockerContainerController(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public AuthConfig authConfig() {
        return dockerClient.authConfig();
    }

    @Override
    public StartContainerCmd startContainerCmd(String containerId) {
        return dockerClient.startContainerCmd(containerId);
    }

    @Override
    public InspectContainerCmd inspectContainerCmd(String containerId) {
        return dockerClient.inspectContainerCmd(containerId);
    }

    @Override
    public ListContainersCmd listContainersCmd() {
        return dockerClient.listContainersCmd();
    }

    @Override
    public ConnectToNetworkCmd connectToNetworkCmd() {
        return dockerClient.connectToNetworkCmd();
    }

    @Override
    public CreateContainerCmd createContainerCmd(String dockerImageName) {
        return dockerClient.createContainerCmd(dockerImageName);
    }

    @Override
    public CopyArchiveFromContainerCmd copyArchiveFromContainerCmd(String containerId, String newRecordingFileName) {
        return dockerClient.copyArchiveFromContainerCmd(containerId, newRecordingFileName);
    }

    @Override
    public WaitContainerCmd waitContainerCmd(String containerId) {
        return dockerClient.waitContainerCmd(containerId);
    }
}
