package org.testcontainers.providers.kubernetes;

import org.testcontainers.controller.ContainerController;
import org.testcontainers.controller.UnsupportedProviderOperationException;
import org.testcontainers.controller.intents.BuildImageIntent;
import org.testcontainers.controller.intents.ConnectToNetworkIntent;
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
import org.testcontainers.providers.kubernetes.intents.BuildImageK8sIntent;
import org.testcontainers.providers.kubernetes.intents.CopyArchiveToContainerK8sIntent;
import org.testcontainers.providers.kubernetes.intents.CreateContainerK8sIntent;
import org.testcontainers.providers.kubernetes.intents.CreateNetworkK8sIntent;
import org.testcontainers.providers.kubernetes.intents.ExecCreateK8sIntent;
import org.testcontainers.providers.kubernetes.intents.ExecStartK8sIntent;
import org.testcontainers.providers.kubernetes.intents.InspectContainerK8sIntent;
import org.testcontainers.providers.kubernetes.intents.InspectExecK8sIntent;
import org.testcontainers.providers.kubernetes.intents.KillContainerK8sIntent;
import org.testcontainers.providers.kubernetes.intents.LogContainerK8sIntent;
import org.testcontainers.providers.kubernetes.intents.RemoveContainerK8sIntent;
import org.testcontainers.providers.kubernetes.intents.StartContainerK8sIntent;
import org.testcontainers.providers.kubernetes.networking.NetworkStrategy;
import org.testcontainers.providers.kubernetes.networking.NodePortStrategy;
import org.testcontainers.providers.kubernetes.repository.RepositoryStrategy;

import java.io.InputStream;

public class KubernetesContainerController implements ContainerController {

    private final KubernetesContext ctx;
    private final NetworkStrategy networkStrategy = new NodePortStrategy();
    private final RepositoryStrategy repositoryStrategy;

    public KubernetesContainerController(
        KubernetesContext ctx,
        RepositoryStrategy repositoryStrategy
    ) {
        this.ctx = ctx;
        this.repositoryStrategy = repositoryStrategy;
    }

    @Override
    public void warmup() {
       ctx.getClient().pods().inNamespace(ctx.getNamespaceProvider().getNamespace()).list();
    }

    @Override
    public String getExposedPortsAddress() {
        return ctx.getNodePortAddress().get();
    }

    @Override
    public String getRandomImageName() {
        return this.repositoryStrategy.getRandomImageName();
    }

    @Override
    public CreateContainerIntent createContainerIntent(String containerImageName) {
        return new CreateContainerK8sIntent(ctx, networkStrategy, containerImageName);
    }

    @Override
    public StartContainerIntent startContainerIntent(String containerId) {
        return new StartContainerK8sIntent(ctx, ctx.findReplicaSet(containerId));
    }

    @Override
    public InspectContainerIntent inspectContainerIntent(String containerId) {
        return new InspectContainerK8sIntent(ctx, networkStrategy, containerId, ctx.findReplicaSet(containerId));
    }

    @Override
    public ListContainersIntent listContainersIntent() {
        return null;
    }

    @Override
    public ConnectToNetworkIntent connectToNetworkIntent() {
        return null;
    }

    @Override
    public CopyArchiveFromContainerIntent copyArchiveFromContainerIntent(String containerId, String newRecordingFileName) {
        return null;
    }

    @Override
    public WaitContainerIntent waitContainerIntent(String containerId) {
        return null;
    }

    @Override
    public TagImageIntent tagImageIntent(String sourceImage, String repositoryWithImage, String tag) {
        return null;
    }

    @Override
    public LogContainerIntent logContainerIntent(String containerId) {
        return new LogContainerK8sIntent(ctx, containerId);
    }

    @Override
    public void checkAndPullImage(String imageName) {
        throw new RuntimeException("Not implemented"); // TODO Implement!
    }

    @Override
    public InspectImageIntent inspectImageIntent(String asCanonicalNameString) throws UnsupportedProviderOperationException {
        throw new UnsupportedProviderOperationException();
    }

    @Override
    public ListImagesIntent listImagesIntent() throws UnsupportedProviderOperationException {
        throw new UnsupportedProviderOperationException();
    }

    @Override
    public PullImageIntent pullImageIntent(String repository) throws UnsupportedProviderOperationException {
        throw new UnsupportedProviderOperationException();
    }

    @Override
    public KillContainerIntent killContainerIntent(String containerId) {
        return new KillContainerK8sIntent(ctx, ctx.findReplicaSet(containerId));
    }

    @Override
    public RemoveContainerIntent removeContainerIntent(String containerId) {
        return new RemoveContainerK8sIntent(ctx, networkStrategy, ctx.findReplicaSet(containerId));
    }

    @Override
    public ListNetworksIntent listNetworksIntent() {
        throw new RuntimeException("Not implemented"); // TODO Implement!
    }

    @Override
    public RemoveNetworkIntent removeNetworkIntent(String id) {
        throw new RuntimeException("Not implemented"); // TODO Implement!
    }

    @Override
    public RemoveImageIntent removeImageIntent(String imageReference) {
        throw new RuntimeException("Not implemented"); // TODO Implement!
    }

    @Override
    public ExecCreateIntent execCreateIntent(String containerId) {
        return new ExecCreateK8sIntent(ctx, containerId);
    }

    @Override
    public ExecStartIntent execStartIntent(String commandId) {
        return new ExecStartK8sIntent(
            ctx,
            commandId,
            ctx.getCommand(commandId)
        );
    }

    @Override
    public InspectExecIntent inspectExecIntent(String commandId) {
        return new InspectExecK8sIntent(
            ctx,
            ctx.getCommand(commandId),
            ctx.getCommandWatch(commandId)
        );
    }

    @Override
    public CopyArchiveToContainerIntent copyArchiveToContainerIntent(String containerId) {
        return new CopyArchiveToContainerK8sIntent(ctx, ctx.findPodForContainerId(containerId));
    }

    @Override
    public BuildImageIntent buildImageIntent(InputStream in) {
        return new BuildImageK8sIntent(ctx, in);
    }

    @Override
    public CreateNetworkIntent createNetworkIntent() {
        return new CreateNetworkK8sIntent();
    }

    @Override
    public KubernetesResourceReaper getResourceReaper() {
        return ctx.getResourceReaper();
    }

}
