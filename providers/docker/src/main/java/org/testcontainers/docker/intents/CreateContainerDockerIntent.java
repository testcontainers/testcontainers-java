package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.model.VolumesFrom;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.controller.intents.CreateContainerIntent;
import org.testcontainers.controller.intents.CreateContainerResult;
import org.testcontainers.controller.model.EnvironmentVariable;
import org.testcontainers.controller.model.HostMount;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateContainerDockerIntent implements CreateContainerIntent {

    private final CreateContainerCmd createContainerCmd;


    public CreateContainerDockerIntent(CreateContainerCmd createContainerCmd) {
        this.createContainerCmd = createContainerCmd;
    }


    @Override
    public CreateContainerIntent withCmd(String... commandParts) {
        createContainerCmd.withCmd(commandParts);
        return this;
    }


    @Override
    public CreateContainerIntent withExposedPorts(ExposedPort... exposedPorts) {
        createContainerCmd.withExposedPorts(exposedPorts);
        return this;
    }

    @Override
    public CreateContainerIntent withExposedPorts(List<ExposedPort> exposedPorts) {
        createContainerCmd.withExposedPorts(exposedPorts);
        return this;
    }

    @Override
    public CreateContainerIntent withHostConfig(HostConfig hostConfig) {
        createContainerCmd.withHostConfig(hostConfig);
        return this;
    }

    @Override
    public HostConfig getHostConfig() {
        return createContainerCmd.getHostConfig();
    }

    @Override
    public CreateContainerIntent withEnv(EnvironmentVariable... environmentVariables) {
        String[] envArray = Arrays.stream(environmentVariables)
            .filter(it -> it.getValue() != null)
            .map(it -> it.getName() + "=" + it.getValue())
            .toArray(String[]::new);
        createContainerCmd.withEnv(envArray);
        return this;
    }

    @Override
    public CreateContainerIntent withHostMounts(HostMount... hostMounts) {
        return this.withHostMounts(Arrays.asList(hostMounts));
    }

    @Override
    public CreateContainerIntent withHostMounts(List<HostMount> hostMounts) {

        createContainerCmd.withBinds(hostMounts.stream().map(m -> new Bind(
                m.getHostPath(),
                new Volume(m.getMountPoint().getPath()),
                m.getMountPoint().getBindMode().accessMode,
                m.getSelContext()
            )).collect(Collectors.toList())
        );
        return this;
    }

    @Override
    public CreateContainerIntent withVolumesFrom(VolumesFrom[] volumesFromsArray) {
        createContainerCmd.withVolumesFrom(volumesFromsArray);
        return this;
    }

    @Override
    public CreateContainerIntent withLinks(Link[] links) {
        createContainerCmd.withLinks(links);
        return this;
    }

    @Override
    public String getNetworkMode() {
        return createContainerCmd.getNetworkMode();
    }

    @Override
    public CreateContainerIntent withNetworkMode(String networkMode) {
        createContainerCmd.withNetworkMode(networkMode);
        return this;
    }

    @Override
    public CreateContainerIntent withExtraHosts(String[] extraHosts) {
        createContainerCmd.withExtraHosts(extraHosts);
        return this;
    }

    @Override
    public CreateContainerIntent withAliases(List<String> networkAliases) {
        createContainerCmd.withAliases(networkAliases);
        return this;
    }

    @Override
    public CreateContainerIntent withWorkingDir(String workingDirectory) {
        createContainerCmd.withWorkingDir(workingDirectory);
        return this;
    }

    @Override
    public CreateContainerIntent withPrivileged(boolean privilegedMode) {
        createContainerCmd.withPrivileged(privilegedMode);
        return this;
    }

    @Override
    public CreateContainerIntent withHostName(String hostName) {
        createContainerCmd.withHostName(hostName);
        return this;
    }

    @Override
    public @NotNull Map<String, String> getLabels() {
        Map<String, String> labels = createContainerCmd.getLabels();
        if (labels != null) {
            return labels;
        }
        return new HashMap<>();
    }

    @Override
    public CreateContainerIntent withLabels(Map<String, String> labels) {
        createContainerCmd.withLabels(labels);
        return this;
    }

    @Override
    public CreateContainerIntent withEntrypoint(String entrypoint) {
        createContainerCmd.withEntrypoint(entrypoint);
        return this;
    }

    @Override
    public CreateContainerIntent withName(String name) {
        createContainerCmd.withName(name);
        return this;
    }

    @Override
    public CreateContainerIntent withAttachStdin(boolean withStdIn) {
        createContainerCmd.withAttachStdin(withStdIn);
        return this;
    }

    @Override
    public CreateContainerIntent withStdinOpen(boolean withStdinOpen) {
        createContainerCmd.withStdinOpen(withStdinOpen);
        return this;
    }

    @Override
    public CreateContainerIntent withTty(boolean ttyEnabled) {
        createContainerCmd.withTty(ttyEnabled);
        return this;
    }

    @Override
    public CreateContainerIntent withCapAdd(Capability cap) {
        createContainerCmd.withCapAdd(cap);
        return this;
    }

    @Override
    public CreateContainerResult perform() {
        return new CreateContainerDockerResult(createContainerCmd.exec());
    }
}
