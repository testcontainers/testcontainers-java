package org.testcontainers.providers.kubernetes.intents;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.VolumesFrom;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.controller.intents.CreateContainerIntent;
import org.testcontainers.controller.intents.CreateContainerResult;
import org.testcontainers.providers.kubernetes.KubernetesContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateContainerK8sIntent implements CreateContainerIntent {

    private final KubernetesContext ctx;
    private final ContainerBuilder containerBuilder = new ContainerBuilder();
    private final ReplicaSetBuilder replicaSetBuilder = new ReplicaSetBuilder();

    public CreateContainerK8sIntent(KubernetesContext ctx, String imageName) {
        this.ctx = ctx;
        containerBuilder.withImage(imageName);
    }


    @Override
    public CreateContainerIntent withCmd(String... args) {
        return null;
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
        return withExposedPorts(Arrays.stream(exposedPorts).collect(Collectors.toList())); // TODO: Refactor
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
    public CreateContainerIntent withEnv(String[] envArray) {
        return null;
    }

    @Override
    public CreateContainerIntent withBinds(Bind[] bindsArray) {
        return null;
    }

    @Override
    public CreateContainerIntent withBinds(List<Bind> binds) {
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
    public CreateContainerResult perform() {
        Map<String, String> identifierLabels = new HashMap<>();
        identifierLabels.put("testcontainers-uuid", UUID.randomUUID().toString());


        Container container = buildContainer();

        // @formatter:off
        replicaSetBuilder
            .editOrNewMetadata()
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

        if (replicaSetBuilder.editOrNewMetadata().getNamespace() == null) {
            replicaSetBuilder.editOrNewMetadata().withNamespace(ctx.getNamespaceProvider().getNamespace()).endMetadata();
        }

        if (replicaSetBuilder.editOrNewMetadata().getName() == null) {
            replicaSetBuilder.editOrNewMetadata().withGenerateName("testcontainers-").endMetadata();
        }


        ReplicaSet replicaSet = replicaSetBuilder.build();

        ReplicaSet createdReplicaSet = ctx.getClient().apps().replicaSets().create(replicaSet);

        if(container.getPorts() != null && !container.getPorts().isEmpty()) {
            ServiceBuilder serviceBuilder = new ServiceBuilder();
            // @formatter:off
            serviceBuilder
                .editOrNewMetadata()
                    .withName(createdReplicaSet.getMetadata().getName())
                    .withNamespace(createdReplicaSet.getMetadata().getNamespace())
                .endMetadata()
                .editOrNewSpec()
                    .withType("NodePort")
                    .withSelector(identifierLabels)
                .endSpec();
            // @formatter:on
            for (ContainerPort containerPort : container.getPorts()) {
                // @formatter:off
                serviceBuilder.editOrNewSpec()
                    .addNewPort()
                        .withName(String.format("%s-%d", containerPort.getProtocol().toLowerCase(), containerPort.getContainerPort()))
                        .withProtocol(containerPort.getProtocol())
                        .withTargetPort(new IntOrString(containerPort.getContainerPort()))
                        .withPort(containerPort.getContainerPort())
                    .endPort()
                .endSpec();
                // @formatter:on
            }
            Service service = ctx.getClient().services().create(serviceBuilder.build());
        }

        return new CreateContainerK8sResult(ctx, createdReplicaSet);
    }
}
