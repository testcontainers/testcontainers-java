package org.testcontainers.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.TagImageCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.NotFoundException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.controller.intents.ConnectToNetworkIntent;
import org.testcontainers.controller.ContainerController;
import org.testcontainers.controller.intents.CreateContainerIntent;
import org.testcontainers.controller.intents.InspectContainerIntent;
import org.testcontainers.controller.intents.ListContainersIntent;
import org.testcontainers.controller.intents.StartContainerIntent;
import org.testcontainers.docker.intents.ConnectToNetworkDockerIntent;
import org.testcontainers.docker.intents.CreateContainerDockerIntent;
import org.testcontainers.docker.intents.InspectContainerDockerIntent;
import org.testcontainers.docker.intents.StartContainerDockerIntent;
import org.testcontainers.images.TimeLimitedLoggedPullImageResultCallback;

@Slf4j
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
    public InspectContainerIntent inspectContainerIntent(String containerId) {
        return new InspectContainerDockerIntent(dockerClient.inspectContainerCmd(containerId));
    }

    @Override
    public ListContainersIntent listContainersIntent() {
        return new ListContainersDockerIntent(dockerClient.listContainersCmd());
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

    @Override
    @SneakyThrows
    public void checkAndPullImage(String imageName) {
        try {
            dockerClient.inspectImageCmd(imageName).exec();
        } catch (NotFoundException notFoundException) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(imageName);
            try {
                pullImageCmd.exec(new TimeLimitedLoggedPullImageResultCallback(log)).awaitCompletion();
            } catch (DockerClientException | InterruptedException e) {
                // Try to fallback to x86
                pullImageCmd
                    .withPlatform("linux/amd64")
                    .exec(new TimeLimitedLoggedPullImageResultCallback(log))
                    .awaitCompletion();
            }
        }
    }

    @Override
    public TagImageCmd tagImageCmd(String sourceImage, String repositoryWithImage, String tag) {
        return dockerClient.tagImageCmd(sourceImage, repositoryWithImage, tag);
    }

    @Override
    public LogContainerCmd logContainerCmd(String containerId) {
        return dockerClient.logContainerCmd(containerId);
    }
}
