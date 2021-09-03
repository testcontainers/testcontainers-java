package org.testcontainers.providers.kubernetes.intents;

import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.VolumesFrom;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.controller.intents.CreateContainerIntent;
import org.testcontainers.controller.intents.CreateContainerResult;
import org.testcontainers.controller.model.EnvironmentVariable;
import org.testcontainers.controller.model.HostMount;
import org.testcontainers.providers.kubernetes.KubernetesContext;
import org.testcontainers.providers.kubernetes.KubernetesExecutionLimitException;
import org.testcontainers.providers.kubernetes.mounts.CopyContentsHostMountStrategy;
import org.testcontainers.providers.kubernetes.mounts.KubernetesHostMountStrategy;
import org.testcontainers.providers.kubernetes.networking.NetworkStrategy;
import org.testcontainers.providers.kubernetes.networking.NetworkingInitParameters;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CreateContainerK8sIntent implements CreateContainerIntent {

    private final KubernetesContext ctx;
    private final ContainerBuilder containerBuilder = new ContainerBuilder();
    private final ReplicaSetBuilder replicaSetBuilder = new ReplicaSetBuilder();
    private final KubernetesHostMountStrategy hostMountStrategy = new CopyContentsHostMountStrategy();
    private final NetworkStrategy networkStrategy;
    private List<String> networkAliases;

    public CreateContainerK8sIntent(KubernetesContext ctx, NetworkStrategy networkStrategy, String imageName) {
        this.ctx = ctx;
        this.networkStrategy = networkStrategy;
        containerBuilder.withImage(imageName);
    }


    @Override
    public CreateContainerIntent withCmd(String... args) {
        containerBuilder.withArgs(args); // Args is the correct property to be set // TODO: Consider renaming interface method
        return this;
    }

    @Override
    public CreateContainerIntent withExposedPorts(List<ExposedPort> exposedPorts) {
        containerBuilder.withPorts(
            exposedPorts.stream()
                .map(dockerPort -> new ContainerPortBuilder()
                    .withContainerPort(dockerPort.getPort())
                    .withProtocol(dockerPort.getProtocol().name())
                    .build()
                )
                .collect(Collectors.toList())
        );
        return this;
    }

    @Override
    public CreateContainerIntent withExposedPorts(ExposedPort... exposedPorts) {
        return withExposedPorts(Arrays.asList(exposedPorts));
    }

    @Override
    public CreateContainerIntent withHostConfig(HostConfig hostConfig) {
        return null;
    }

    @Override
    public HostConfig getHostConfig() {
        return null;
    }

    @Override
    public CreateContainerIntent withEnv(EnvironmentVariable... environmentVariables) {
        containerBuilder.withEnv(
            Stream.of(environmentVariables)
                .map(e -> new EnvVar(e.getName(), e.getValue(), null))
                .collect(Collectors.toList())
        );
        return this;
    }

    @Override
    public CreateContainerIntent withHostMounts(HostMount... hostMounts) {
        return withHostMounts(Arrays.asList(hostMounts));
    }

    @Override
    public CreateContainerIntent withHostMounts(List<HostMount> hostMounts) {
        hostMountStrategy.withHostMounts(hostMounts);
        return null;
    }

    @Override
    public CreateContainerIntent withVolumesFrom(VolumesFrom[] volumesFromsArray) {
        return null;
    }

    @Override
    public CreateContainerIntent withLinks(Link[] links) {
        return null;
    }

    @Override
    public String getNetworkMode() {
        return null;
    }

    @Override
    public CreateContainerIntent withNetworkMode(String networkMode) {
        return null;
    }

    @Override
    public CreateContainerIntent withExtraHosts(String[] extraHosts) {
        return null;
    }

    @Override
    public CreateContainerIntent withAliases(List<String> networkAliases) {
        this.networkAliases = networkAliases;
        return null;
    }

    @Override
    public CreateContainerIntent withWorkingDir(String workingDirectory) {
        return null;
    }

    @Override
    public CreateContainerIntent withPrivileged(boolean privilegedMode) {
        return null;
    }

    @Override
    public CreateContainerIntent withHostName(String hostName) {
        return null;
    }

    @Override
    public @NotNull Map<String, String> getLabels() {
        Map<String, String> labels = replicaSetBuilder.editOrNewMetadata().getLabels();
        if (labels == null) {
            labels = new HashMap<>();
        }
        return labels;
    }

    @Override
    public CreateContainerIntent withLabels(Map<String, String> labels) {
        replicaSetBuilder.editOrNewMetadata().withLabels(labels).endMetadata();
        return this;
    }

    @Override
    public CreateContainerIntent withEntrypoint(String entrypoint) {
        return null;
    }

    @Override
    public CreateContainerIntent withName(String name) {
        return null;
    }

    @Override
    public CreateContainerIntent withAttachStdin(boolean withStdIn) {
        return null;
    }

    @Override
    public CreateContainerIntent withStdinOpen(boolean withStdinOpen) {
        return null;
    }

    @Override
    public CreateContainerIntent withTty(boolean ttyEnabled) {
        return null;
    }

    @Override
    public CreateContainerIntent withCapAdd(Capability capability) {
        return null;
    }


    private Container buildContainer() {
        if (containerBuilder.getName() == null) {
            containerBuilder.withName("testcontainer");
        }

        return containerBuilder.build();
    }

    @Override
    @SneakyThrows({KubernetesExecutionLimitException.class})
    public CreateContainerResult perform() {
        Map<String, String> identifierLabels = new HashMap<>();
        String uuid = UUID.randomUUID().toString();
        identifierLabels.put("testcontainers-uuid", uuid);


        Container container = buildContainer();

        // @formatter:off
        replicaSetBuilder
            .editOrNewMetadata()
                .withNamespace(ctx.getNamespaceProvider().getNamespace())
                .addToLabels(identifierLabels)
            .endMetadata()
            .editOrNewSpec()
                .withNewSelector()
                    .withMatchLabels(identifierLabels)
                .endSelector()
                .editOrNewTemplate()
                    .editOrNewMetadata()
                        .addToLabels(identifierLabels)
                    .endMetadata()
                    .editOrNewSpec()
                        .addNewContainerLike(container)
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec();
        // @formatter:on


        if (replicaSetBuilder.editOrNewMetadata().getName() == null) {
            replicaSetBuilder.editOrNewMetadata().withGenerateName("testcontainers-").endMetadata();
        }

        hostMountStrategy
            .configure(replicaSetBuilder.editOrNewSpec().editOrNewTemplate().editOrNewSpec())
            .endSpec()
            .endTemplate()
            .endSpec();

        ReplicaSet replicaSet = replicaSetBuilder.build();

        ctx.getExecutionLimiter().checkLimits(ctx, replicaSet);


        ReplicaSet createdReplicaSet = ctx.getClient().apps().replicaSets().create(replicaSet);

        networkStrategy.apply(ctx, new NetworkingInitParameters(
            createdReplicaSet.getMetadata().getNamespace(),
            createdReplicaSet.getMetadata().getName(),
            identifierLabels,
            container,
            networkAliases == null ? Collections.emptySet() : networkAliases.stream().distinct().collect(Collectors.toSet())
        ));

        // TODO: Handle errors after replicaset creation

        Pod pod = ctx.findPodForReplicaSet(replicaSet);

        hostMountStrategy.apply(ctx, pod);

        ctx.getClient().pods()
            .inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName())
            .waitUntilReady(3, TimeUnit.MINUTES); // TODO: Maybe just wait for container status. See below
            // TODO: Configurable timeout?
        /*
        Pulling:
        =========================
  containerStatuses:
  - image: couchbase/server:enterprise-6.6.2
    imageID: ""
    lastState: {}
    name: testcontainer
    ready: false
    restartCount: 0
    started: false
    state:
      waiting:
        reason: ContainerCreating

        Pulled:
        =================
  containerStatuses:
  - containerID: docker://b657ad66dd4fabfa285d037030ca4369eb0da301911c870651425d766415b92e
    image: couchbase/server:community-6.6.0
    imageID: docker-pullable://couchbase/server@sha256:8d4d340ee73060bdecbe8bc5ca9dba390c5336d9d7c4e5c0319957fb5960f61d
    lastState: {}
    name: testcontainer
    ready: true
    restartCount: 0
    started: true
    state:
      running:
        startedAt: "2021-08-29T08:48:43Z"

         */




        return new CreateContainerK8sResult(ctx, createdReplicaSet);
    }
}
