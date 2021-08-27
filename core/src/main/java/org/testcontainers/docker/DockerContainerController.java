package org.testcontainers.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import org.testcontainers.controller.ConnectToNetworkIntent;
import org.testcontainers.controller.ContainerController;
import org.testcontainers.controller.CreateContainerIntent;
import org.testcontainers.controller.StartContainerIntent;

public class DockerContainerController implements ContainerController {


    private final DockerClient dockerClient;

    public DockerContainerController(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public void warmup() {
        // trigger LazyDockerClient's resolve so that we fail fast here and not in getDockerImageName()
        dockerClient.authConfig();
    }

    @Override
    public StartContainerIntent startContainerIntent(String containerId) {
        return new StartContainerDockerIntent(dockerClient.startContainerCmd(containerId));
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
    public ConnectToNetworkIntent connectToNetworkIntent() {
        return new ConnectToNetworkDockerIntent(dockerClient.connectToNetworkCmd());
    }

    @Override
    public CreateContainerIntent createContainerIntent(String containerImageName) {
        return new CreateContainerDockerIntent(dockerClient.createContainerCmd(containerImageName));
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
