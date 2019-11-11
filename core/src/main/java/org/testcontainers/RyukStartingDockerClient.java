package org.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.AttachContainerCmd;
import com.github.dockerjava.api.command.AuthCmd;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.CommitCmd;
import com.github.dockerjava.api.command.ConnectToNetworkCmd;
import com.github.dockerjava.api.command.ContainerDiffCmd;
import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import com.github.dockerjava.api.command.CopyFileFromContainerCmd;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateImageCmd;
import com.github.dockerjava.api.command.CreateNetworkCmd;
import com.github.dockerjava.api.command.CreateServiceCmd;
import com.github.dockerjava.api.command.CreateVolumeCmd;
import com.github.dockerjava.api.command.DisconnectFromNetworkCmd;
import com.github.dockerjava.api.command.EventsCmd;
import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.api.command.InfoCmd;
import com.github.dockerjava.api.command.InitializeSwarmCmd;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectExecCmd;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectNetworkCmd;
import com.github.dockerjava.api.command.InspectServiceCmd;
import com.github.dockerjava.api.command.InspectSwarmCmd;
import com.github.dockerjava.api.command.InspectVolumeCmd;
import com.github.dockerjava.api.command.JoinSwarmCmd;
import com.github.dockerjava.api.command.KillContainerCmd;
import com.github.dockerjava.api.command.LeaveSwarmCmd;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.command.ListNetworksCmd;
import com.github.dockerjava.api.command.ListServicesCmd;
import com.github.dockerjava.api.command.ListSwarmNodesCmd;
import com.github.dockerjava.api.command.ListTasksCmd;
import com.github.dockerjava.api.command.ListVolumesCmd;
import com.github.dockerjava.api.command.LoadImageCmd;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.LogSwarmObjectCmd;
import com.github.dockerjava.api.command.PauseContainerCmd;
import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.api.command.PruneCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.RemoveImageCmd;
import com.github.dockerjava.api.command.RemoveNetworkCmd;
import com.github.dockerjava.api.command.RemoveServiceCmd;
import com.github.dockerjava.api.command.RemoveVolumeCmd;
import com.github.dockerjava.api.command.RenameContainerCmd;
import com.github.dockerjava.api.command.RestartContainerCmd;
import com.github.dockerjava.api.command.SaveImageCmd;
import com.github.dockerjava.api.command.SearchImagesCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.command.TagImageCmd;
import com.github.dockerjava.api.command.TopContainerCmd;
import com.github.dockerjava.api.command.UnpauseContainerCmd;
import com.github.dockerjava.api.command.UpdateContainerCmd;
import com.github.dockerjava.api.command.UpdateServiceCmd;
import com.github.dockerjava.api.command.UpdateSwarmCmd;
import com.github.dockerjava.api.command.UpdateSwarmNodeCmd;
import com.github.dockerjava.api.command.VersionCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Identifier;
import com.github.dockerjava.api.model.PruneType;
import com.github.dockerjava.api.model.ServiceSpec;
import com.github.dockerjava.api.model.SwarmSpec;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.testcontainers.utility.ResourceReaper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Note: this class is not using {@link lombok.experimental.Delegate}
 * to force us implementing new methods when we update docker-java.
 */
@FieldDefaults(makeFinal = true)
@RequiredArgsConstructor
class RyukStartingDockerClient implements DockerClient {

    DockerClient dockerClient;

    String hostIpAddress;

    void ensureRyukIsRunning() {
        ResourceReaper.start(hostIpAddress, dockerClient);
    }

    @Override
    public StartContainerCmd startContainerCmd(String containerId) {
        ensureRyukIsRunning();
        return dockerClient.startContainerCmd(containerId);
    }

    @Override
    public CreateNetworkCmd createNetworkCmd() {
        ensureRyukIsRunning();
        return dockerClient.createNetworkCmd();
    }

    @Override
    public CreateImageCmd createImageCmd(String repository, InputStream imageStream) {
        ensureRyukIsRunning();
        return dockerClient.createImageCmd(repository, imageStream);
    }

    @Override
    public BuildImageCmd buildImageCmd() {
        ensureRyukIsRunning();
        return dockerClient.buildImageCmd();
    }

    @Override
    public BuildImageCmd buildImageCmd(File dockerFileOrFolder) {
        ensureRyukIsRunning();
        return dockerClient.buildImageCmd(dockerFileOrFolder);
    }

    @Override
    public BuildImageCmd buildImageCmd(InputStream tarInputStream) {
        ensureRyukIsRunning();
        return dockerClient.buildImageCmd(tarInputStream);
    }

    @Override
    public CreateVolumeCmd createVolumeCmd() {
        ensureRyukIsRunning();
        return dockerClient.createVolumeCmd();
    }

    @Override
    public CreateServiceCmd createServiceCmd(ServiceSpec serviceSpec) {
        ensureRyukIsRunning();
        return dockerClient.createServiceCmd(serviceSpec);
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // Delegating methods follow
    ////////////////////////////////////////////////////////////////////////////////////

    @Override
    public AuthConfig authConfig() throws DockerException {
        return dockerClient.authConfig();
    }

    @Override
    public AuthCmd authCmd() {
        return dockerClient.authCmd();
    }

    @Override
    public InfoCmd infoCmd() {
        return dockerClient.infoCmd();
    }

    @Override
    public PingCmd pingCmd() {
        return dockerClient.pingCmd();
    }

    @Override
    public VersionCmd versionCmd() {
        return dockerClient.versionCmd();
    }

    @Override
    public PullImageCmd pullImageCmd(String repository) {
        return dockerClient.pullImageCmd(repository);
    }

    @Override
    public PushImageCmd pushImageCmd(String name) {
        return dockerClient.pushImageCmd(name);
    }

    @Override
    public PushImageCmd pushImageCmd(Identifier identifier) {
        return dockerClient.pushImageCmd(identifier);
    }

    @Override
    public LoadImageCmd loadImageCmd(InputStream imageStream) {
        return dockerClient.loadImageCmd(imageStream);
    }

    @Override
    public SearchImagesCmd searchImagesCmd(String term) {
        return dockerClient.searchImagesCmd(term);
    }

    @Override
    public RemoveImageCmd removeImageCmd(String imageId) {
        return dockerClient.removeImageCmd(imageId);
    }

    @Override
    public ListImagesCmd listImagesCmd() {
        return dockerClient.listImagesCmd();
    }

    @Override
    public InspectImageCmd inspectImageCmd(String imageId) {
        return dockerClient.inspectImageCmd(imageId);
    }

    @Override
    public SaveImageCmd saveImageCmd(String name) {
        return dockerClient.saveImageCmd(name);
    }

    @Override
    public ListContainersCmd listContainersCmd() {
        return dockerClient.listContainersCmd();
    }

    @Override
    public CreateContainerCmd createContainerCmd(String image) {
        return dockerClient.createContainerCmd(image);
    }

    @Override
    public ExecCreateCmd execCreateCmd(String containerId) {
        return dockerClient.execCreateCmd(containerId);
    }

    @Override
    public InspectContainerCmd inspectContainerCmd(String containerId) {
        return dockerClient.inspectContainerCmd(containerId);
    }

    @Override
    public RemoveContainerCmd removeContainerCmd(String containerId) {
        return dockerClient.removeContainerCmd(containerId);
    }

    @Override
    public WaitContainerCmd waitContainerCmd(String containerId) {
        return dockerClient.waitContainerCmd(containerId);
    }

    @Override
    public AttachContainerCmd attachContainerCmd(String containerId) {
        return dockerClient.attachContainerCmd(containerId);
    }

    @Override
    public ExecStartCmd execStartCmd(String execId) {
        return dockerClient.execStartCmd(execId);
    }

    @Override
    public InspectExecCmd inspectExecCmd(String execId) {
        return dockerClient.inspectExecCmd(execId);
    }

    @Override
    public LogContainerCmd logContainerCmd(String containerId) {
        return dockerClient.logContainerCmd(containerId);
    }

    @Override
    public CopyArchiveFromContainerCmd copyArchiveFromContainerCmd(String containerId, String resource) {
        return dockerClient.copyArchiveFromContainerCmd(containerId, resource);
    }

    @Override
    @Deprecated
    public CopyFileFromContainerCmd copyFileFromContainerCmd(String containerId, String resource) {
        return dockerClient.copyFileFromContainerCmd(containerId, resource);
    }

    @Override
    public CopyArchiveToContainerCmd copyArchiveToContainerCmd(String containerId) {
        return dockerClient.copyArchiveToContainerCmd(containerId);
    }

    @Override
    public ContainerDiffCmd containerDiffCmd(String containerId) {
        return dockerClient.containerDiffCmd(containerId);
    }

    @Override
    public StopContainerCmd stopContainerCmd(String containerId) {
        return dockerClient.stopContainerCmd(containerId);
    }

    @Override
    public KillContainerCmd killContainerCmd(String containerId) {
        return dockerClient.killContainerCmd(containerId);
    }

    @Override
    public UpdateContainerCmd updateContainerCmd(String containerId) {
        return dockerClient.updateContainerCmd(containerId);
    }

    @Override
    public RenameContainerCmd renameContainerCmd(String containerId) {
        return dockerClient.renameContainerCmd(containerId);
    }

    @Override
    public RestartContainerCmd restartContainerCmd(String containerId) {
        return dockerClient.restartContainerCmd(containerId);
    }

    @Override
    public CommitCmd commitCmd(String containerId) {
        return dockerClient.commitCmd(containerId);
    }

    @Override
    public TopContainerCmd topContainerCmd(String containerId) {
        return dockerClient.topContainerCmd(containerId);
    }

    @Override
    public TagImageCmd tagImageCmd(String imageId, String imageNameWithRepository, String tag) {
        return dockerClient.tagImageCmd(imageId, imageNameWithRepository, tag);
    }

    @Override
    public PauseContainerCmd pauseContainerCmd(String containerId) {
        return dockerClient.pauseContainerCmd(containerId);
    }

    @Override
    public UnpauseContainerCmd unpauseContainerCmd(String containerId) {
        return dockerClient.unpauseContainerCmd(containerId);
    }

    @Override
    public EventsCmd eventsCmd() {
        return dockerClient.eventsCmd();
    }

    @Override
    public StatsCmd statsCmd(String containerId) {
        return dockerClient.statsCmd(containerId);
    }

    @Override
    public InspectVolumeCmd inspectVolumeCmd(String name) {
        return dockerClient.inspectVolumeCmd(name);
    }

    @Override
    public RemoveVolumeCmd removeVolumeCmd(String name) {
        return dockerClient.removeVolumeCmd(name);
    }

    @Override
    public ListVolumesCmd listVolumesCmd() {
        return dockerClient.listVolumesCmd();
    }

    @Override
    public ListNetworksCmd listNetworksCmd() {
        return dockerClient.listNetworksCmd();
    }

    @Override
    public InspectNetworkCmd inspectNetworkCmd() {
        return dockerClient.inspectNetworkCmd();
    }

    @Override
    public RemoveNetworkCmd removeNetworkCmd(String networkId) {
        return dockerClient.removeNetworkCmd(networkId);
    }

    @Override
    public ConnectToNetworkCmd connectToNetworkCmd() {
        return dockerClient.connectToNetworkCmd();
    }

    @Override
    public DisconnectFromNetworkCmd disconnectFromNetworkCmd() {
        return dockerClient.disconnectFromNetworkCmd();
    }

    @Override
    public InitializeSwarmCmd initializeSwarmCmd(SwarmSpec swarmSpec) {
        return dockerClient.initializeSwarmCmd(swarmSpec);
    }

    @Override
    public InspectSwarmCmd inspectSwarmCmd() {
        return dockerClient.inspectSwarmCmd();
    }

    @Override
    public JoinSwarmCmd joinSwarmCmd() {
        return dockerClient.joinSwarmCmd();
    }

    @Override
    public LeaveSwarmCmd leaveSwarmCmd() {
        return dockerClient.leaveSwarmCmd();
    }

    @Override
    public UpdateSwarmCmd updateSwarmCmd(SwarmSpec swarmSpec) {
        return dockerClient.updateSwarmCmd(swarmSpec);
    }

    @Override
    public UpdateSwarmNodeCmd updateSwarmNodeCmd() {
        return dockerClient.updateSwarmNodeCmd();
    }

    @Override
    public ListSwarmNodesCmd listSwarmNodesCmd() {
        return dockerClient.listSwarmNodesCmd();
    }

    @Override
    public ListServicesCmd listServicesCmd() {
        return dockerClient.listServicesCmd();
    }

    @Override
    public InspectServiceCmd inspectServiceCmd(String serviceId) {
        return dockerClient.inspectServiceCmd(serviceId);
    }

    @Override
    public UpdateServiceCmd updateServiceCmd(String serviceId, ServiceSpec serviceSpec) {
        return dockerClient.updateServiceCmd(serviceId, serviceSpec);
    }

    @Override
    public RemoveServiceCmd removeServiceCmd(String serviceId) {
        return dockerClient.removeServiceCmd(serviceId);
    }

    @Override
    public ListTasksCmd listTasksCmd() {
        return dockerClient.listTasksCmd();
    }

    @Override
    public LogSwarmObjectCmd logServiceCmd(String serviceId) {
        return dockerClient.logServiceCmd(serviceId);
    }

    @Override
    public LogSwarmObjectCmd logTaskCmd(String taskId) {
        return dockerClient.logTaskCmd(taskId);
    }

    @Override
    public PruneCmd pruneCmd(PruneType pruneType) {
        return dockerClient.pruneCmd(pruneType);
    }

    @Override
    public void close() throws IOException {
        dockerClient.close();
    }
}
