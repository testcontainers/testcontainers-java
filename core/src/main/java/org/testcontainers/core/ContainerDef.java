package org.testcontainers.core;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.UnstableAPI;
import org.testcontainers.containers.Network;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@UnstableAPI
@Slf4j
public class ContainerDef {

    private RemoteDockerImage image;

    private final Set<ExposedPort> exposedPorts = new HashSet<>();

    private final Set<PortBinding> portBindings = new HashSet<>();

    private final Map<String, String> labels = new HashMap<>();

    private final Map<String, String> environmentVariables = new HashMap<>();

    private String[] command = new String[0];

    private Network network;

    private final Set<String> networkAliases = new HashSet<>();

    private String networkMode;

    private final List<Bind> binds = new ArrayList<>();

    private boolean privilegedMode;

    public static ContainerDef from(RemoteDockerImage image) {
        return new ContainerDef().withImage(image);
    }

    public static ContainerDef from(String image) {
        return new ContainerDef().withImage(image);
    }

    public static ContainerDef from(DockerImageName image) {
        return new ContainerDef().withImage(image);
    }

    private ContainerDef withImage(RemoteDockerImage image) {
        this.image = image;
        return this;
    }

    private ContainerDef withImage(String image) {
        return withImage(DockerImageName.parse(image));
    }

    private ContainerDef withImage(DockerImageName image) {
        return withImage(new RemoteDockerImage(image));
    }

    public ContainerDef withExposedPorts(Set<ExposedPort> exposedPorts) {
        this.exposedPorts.addAll(exposedPorts);
        return this;
    }

    public ContainerDef setExposedPorts(Set<ExposedPort> exposedPorts) {
        this.exposedPorts.clear();
        this.exposedPorts.addAll(exposedPorts);
        return this;
    }

    public ContainerDef addExposedPorts(ExposedPort... exposedPorts) {
        this.exposedPorts.addAll(Arrays.asList(exposedPorts));
        return this;
    }

    public ContainerDef withExposedPort(ExposedPort exposedPort) {
        this.exposedPorts.add(exposedPort);
        return this;
    }

    public ContainerDef addExposedPort(ExposedPort exposedPort) {
        this.exposedPorts.add(exposedPort);
        return this;
    }

    public ContainerDef withExposedPort(int port) {
        return withExposedPort(new ExposedPort(port));
    }

    public ContainerDef withExposedPort(int port, InternetProtocol protocol) {
        this.exposedPorts.add(new ExposedPort(port, protocol));
        return this;
    }

    public ContainerDef addExposedPort(int port, InternetProtocol protocol) {
        this.exposedPorts.add(new ExposedPort(port, protocol));
        return this;
    }

    public ContainerDef withExposedPorts(int... ports) {
        for (int port : ports) {
            withExposedPort(port);
        }
        return this;
    }

    public ContainerDef withPortBindings(Set<PortBinding> portBindings) {
        this.portBindings.addAll(portBindings);
        return this;
    }

    public ContainerDef setPortBindings(Set<PortBinding> portBindings) {
        this.portBindings.clear();
        this.portBindings.addAll(portBindings);
        return this;
    }

    public ContainerDef addPortBindings(PortBinding... portBindings) {
        this.portBindings.addAll(Arrays.asList(portBindings));
        return this;
    }

    public ContainerDef withLabels(Map<String, String> labels) {
        this.labels.putAll(labels);
        return this;
    }

    public ContainerDef setLabels(Map<String, String> labels) {
        this.labels.clear();
        this.labels.putAll(labels);
        return this;
    }

    public ContainerDef addLabels(Map<String, String> labels) {
        this.labels.putAll(labels);
        return this;
    }

    public ContainerDef addLabel(String key, String value) {
        this.labels.put(key, value);
        return this;
    }

    public ContainerDef withEnvVars(Map<String, String> envVars) {
        this.environmentVariables.putAll(envVars);
        return this;
    }

    public ContainerDef setEnvVars(Map<String, String> envVars) {
        this.environmentVariables.clear();
        this.environmentVariables.putAll(envVars);
        return this;
    }

    public ContainerDef addEnvVars(Map<String, String> envVars) {
        this.environmentVariables.putAll(envVars);
        return this;
    }

    public ContainerDef withEnvVar(String key, String value) {
        this.environmentVariables.put(key, value);
        return this;
    }

    public ContainerDef withCommand(String... command) {
        this.command = command;
        return this;
    }

    public ContainerDef withNetwork(Network network) {
        this.network = network;
        return this;
    }

    public ContainerDef withNetworkAliases(String... aliases) {
        this.networkAliases.addAll(Arrays.asList(aliases));
        return this;
    }

    public ContainerDef setNetworkAliases(String... aliases) {
        this.networkAliases.clear();
        this.networkAliases.addAll(Arrays.asList(aliases));
        return this;
    }

    public ContainerDef withNetworkMode(String networkMode) {
        this.networkMode = networkMode;
        return this;
    }

    public ContainerDef withPrivilegedMode(boolean privilegedMode) {
        this.privilegedMode = privilegedMode;
        return this;
    }

    public void applyTo(CreateContainerCmd createCommand) {
        HostConfig hostConfig = createCommand.getHostConfig();
        if (hostConfig == null) {
            hostConfig = new HostConfig();
            createCommand.withHostConfig(hostConfig);
        }
        // PortBindings must contain:
        //  * all exposed ports with a randomized host port (equivalent to -p CONTAINER_PORT)
        //  * all exposed ports with a fixed host port (equivalent to -p HOST_PORT:CONTAINER_PORT)
        Map<ExposedPort, PortBinding> allPortBindings = new HashMap<>();
        // First, collect all the randomized host ports from our 'exposedPorts' field
        for (ExposedPort exposedPort : this.exposedPorts) {
            allPortBindings.put(exposedPort, new PortBinding(Ports.Binding.empty(), exposedPort));
        }
        // Next, collect all the fixed host ports from our 'portBindings' field, overwriting any randomized ports so that
        // we don't create two bindings for the same container port.
        for (PortBinding portBinding : this.portBindings) {
            allPortBindings.put(portBinding.getExposedPort(), portBinding);
        }
        hostConfig.withPortBindings(new ArrayList<>(allPortBindings.values()));

        // Next, ExposedPorts must be set up to publish all of the above ports, both randomized and fixed.
        createCommand.withExposedPorts(new ArrayList<>(allPortBindings.keySet()));

        createCommand.withEnv(
            this.environmentVariables.entrySet()
                .stream()
                .filter(it -> it.getValue() != null)
                .map(it -> it.getKey() + "=" + it.getValue())
                .toArray(String[]::new)
        );

        if (this.command != null) {
            createCommand.withCmd(this.command);
        }

        if (this.network != null) {
            hostConfig.withNetworkMode(this.network.getId());
            createCommand.withAliases(this.networkAliases.toArray(new String[0]));
        } else {
            if (this.networkMode != null) {
                createCommand.getHostConfig().withNetworkMode(this.networkMode);
            }
        }

        boolean shouldCheckFileMountingSupport =
            this.binds.size() > 0 && !TestcontainersConfiguration.getInstance().isDisableChecks();
        if (shouldCheckFileMountingSupport) {
            if (!DockerClientFactory.instance().isFileMountingSupported()) {
                log.warn(
                    "Unable to mount a file from test host into a running container. " +
                    "This may be a misconfiguration or limitation of your Docker environment. " +
                    "Some features might not work."
                );
            }
        }

        hostConfig.withBinds(this.binds.toArray(new Bind[0]));

        if (this.privilegedMode) {
            createCommand.getHostConfig().withPrivileged(this.privilegedMode);
        }

        Map<String, String> combinedLabels = new HashMap<>(this.labels);
        if (createCommand.getLabels() != null) {
            combinedLabels.putAll(createCommand.getLabels());
        }

        createCommand.withLabels(combinedLabels);
    }
}
