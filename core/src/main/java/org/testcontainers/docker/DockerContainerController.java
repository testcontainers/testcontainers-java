package org.testcontainers.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.NotFoundException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.controller.ResourceCleaner;
import org.testcontainers.controller.intents.BuildImageIntent;
import org.testcontainers.controller.intents.ConnectToNetworkIntent;
import org.testcontainers.controller.ContainerController;
import org.testcontainers.controller.intents.CopyArchiveFromContainerIntent;
import org.testcontainers.controller.intents.CopyArchiveToContainerIntent;
import org.testcontainers.controller.intents.CreateContainerIntent;
import org.testcontainers.controller.intents.CreateNetworkIntent;
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
import org.testcontainers.docker.intents.BuildImageDockerIntent;
import org.testcontainers.docker.intents.ConnectToNetworkDockerIntent;
import org.testcontainers.docker.intents.CopyArchiveFromContainerDockerIntent;
import org.testcontainers.docker.intents.CopyArchiveToContainerDockerIntent;
import org.testcontainers.docker.intents.CreateContainerDockerIntent;
import org.testcontainers.docker.intents.CreateNetworkDockerIntent;
import org.testcontainers.docker.intents.ExecCreateDockerIntent;
import org.testcontainers.docker.intents.ExecStartDockerIntent;
import org.testcontainers.docker.intents.InspectContainerDockerIntent;
import org.testcontainers.docker.intents.InspectExecDockerIntent;
import org.testcontainers.docker.intents.InspectImageDockerIntent;
import org.testcontainers.docker.intents.KillContainerDockerIntent;
import org.testcontainers.docker.intents.ListContainersDockerIntent;
import org.testcontainers.docker.intents.ListImagesDockerIntent;
import org.testcontainers.docker.intents.ListNetworksDockerIntent;
import org.testcontainers.docker.intents.LogContainerDockerIntent;
import org.testcontainers.docker.intents.PullImageDockerIntent;
import org.testcontainers.docker.intents.RemoveContainerDockerIntent;
import org.testcontainers.docker.intents.RemoveImageDockerIntent;
import org.testcontainers.docker.intents.RemoveNetworkDockerIntent;
import org.testcontainers.docker.intents.StartContainerDockerIntent;
import org.testcontainers.docker.intents.TagImageDockerIntent;
import org.testcontainers.docker.intents.WaitContainerDockerIntent;
import org.testcontainers.images.TimeLimitedLoggedPullImageResultCallback;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.ResourceReaper;

import java.io.InputStream;

@Slf4j
public class DockerContainerController implements ContainerController {


    private final DockerClient dockerClient;
    private final ResourceReaper resourceReaper = new ResourceReaper(this);

    public DockerContainerController(
        DockerClient dockerClient
    ) {
        this.dockerClient = dockerClient;
    }

    @Override
    public void warmup() {
        // trigger LazyDockerClient's resolve so that we fail fast here and not in getDockerImageName()
        dockerClient.authConfig();
    }

    @Override
    public String getExposedPortsAddress() {
        return DockerClientFactory.instance().dockerHostIpAddress();
    }

    @Override
    public String getRandomImageName() {
        return "localhost/testcontainers/" + Base58.randomString(16).toLowerCase();
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
    public CopyArchiveFromContainerIntent copyArchiveFromContainerIntent(String containerId, String newRecordingFileName) {
        return new CopyArchiveFromContainerDockerIntent(dockerClient.copyArchiveFromContainerCmd(containerId, newRecordingFileName));
    }

    @Override
    public WaitContainerIntent waitContainerIntent(String containerId) {
        return new WaitContainerDockerIntent(dockerClient.waitContainerCmd(containerId));
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
    public InspectImageIntent inspectImageIntent(String imageId) {
        return new InspectImageDockerIntent(dockerClient.inspectImageCmd(imageId));
    }

    @Override
    public ListImagesIntent listImagesIntent() {
        return new ListImagesDockerIntent(dockerClient.listImagesCmd());
    }

    @Override
    public PullImageIntent pullImageIntent(String repository) {
        return new PullImageDockerIntent(dockerClient.pullImageCmd(repository));
    }

    @Override
    public KillContainerIntent killContainerIntent(String containerId) {
        return new KillContainerDockerIntent(dockerClient.killContainerCmd(containerId));
    }

    @Override
    public RemoveContainerIntent removeContainerIntent(String containerId) {
        return new RemoveContainerDockerIntent(dockerClient.removeContainerCmd(containerId));
    }

    @Override
    public ListNetworksIntent listNetworksIntent() {
        return new ListNetworksDockerIntent(dockerClient.listNetworksCmd());
    }

    @Override
    public RemoveNetworkIntent removeNetworkIntent(String id) {
        return new RemoveNetworkDockerIntent(dockerClient.removeNetworkCmd(id));
    }

    @Override
    public RemoveImageIntent removeImageIntent(String imageId) {
        return new RemoveImageDockerIntent(dockerClient.removeImageCmd(imageId));
    }

    @Override
    public ExecCreateIntent execCreateIntent(String containerId) {
        return new ExecCreateDockerIntent(dockerClient.execCreateCmd(containerId));
    }

    @Override
    public ExecStartIntent execStartIntent(String commandId) {
        return new ExecStartDockerIntent(dockerClient.execStartCmd(commandId));
    }

    @Override
    public InspectExecIntent inspectExecIntent(String commandId) {
        return new InspectExecDockerIntent(dockerClient.inspectExecCmd(commandId));
    }

    @Override
    public CopyArchiveToContainerIntent copyArchiveToContainerIntent(String containerId) {
        return new CopyArchiveToContainerDockerIntent(dockerClient.copyArchiveToContainerCmd(containerId));
    }

    @Override
    public BuildImageIntent buildImageIntent(InputStream in) {
        return new BuildImageDockerIntent(dockerClient.buildImageCmd(in));
    }

    @Override
    public CreateNetworkIntent createNetworkIntent() {
        return new CreateNetworkDockerIntent(dockerClient.createNetworkCmd());
    }

    @Override
    public TagImageIntent tagImageIntent(String sourceImage, String repositoryWithImage, String tag) {
        return new TagImageDockerIntent(dockerClient.tagImageCmd(sourceImage, repositoryWithImage, tag));
    }

    @Override
    public LogContainerIntent logContainerIntent(String containerId) {
        return new LogContainerDockerIntent(dockerClient.logContainerCmd(containerId));
    }

    @Override
    public ResourceCleaner getResourceReaper() {
        return resourceReaper;
    }

    public DockerClient getClient() {
        return dockerClient;
    }
}
