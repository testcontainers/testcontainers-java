package org.testcontainers.core;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.Getter;
import org.testcontainers.UnstableAPI;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@UnstableAPI
public class ContainerDef {

    private RemoteDockerImage image;

    private final Set<ExposedPort> exposedPorts = new HashSet<>();

    private final Set<PortBinding> portBindings = new HashSet<>();

    private final Map<String, String> envVars = new HashMap<>();

    private String[] command = null;

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

    public ContainerDef withExposedPort(ExposedPort exposedPort) {
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

    public ContainerDef withEnvVars(Map<String, String> envVars) {
        this.envVars.putAll(envVars);
        return this;
    }

    public ContainerDef withEnvVar(String key, String value) {
        this.envVars.put(key, value);
        return this;
    }

    public ContainerDef withCommand(String... command) {
        this.command = command;
        return this;
    }

    public void applyTo(CreateContainerCmd createCmd) {
        HostConfig hostConfig = createCmd.getHostConfig();
        if (hostConfig == null) {
            hostConfig = new HostConfig();
            createCmd.withHostConfig(hostConfig);
        }

        // PortBindings must contain:
        //  * all exposed ports with a randomized host port (equivalent to -p CONTAINER_PORT)
        //  * all exposed ports with a fixed host port (equivalent to -p HOST_PORT:CONTAINER_PORT)
        Map<ExposedPort, PortBinding> allPortBindings = new HashMap<>();
        // First collect all the randomized host ports from our 'exposedPorts' field
        for (final ExposedPort exposedPort : this.exposedPorts) {
            allPortBindings.put(exposedPort, new PortBinding(Ports.Binding.empty(), exposedPort));
        }
        // Next collect all the fixed host ports from our 'portBindings' field, overwriting any randomized ports so that
        // we don't create two bindings for the same container port.
        for (final PortBinding portBinding : this.portBindings) {
            allPortBindings.put(portBinding.getExposedPort(), portBinding);
        }
        hostConfig.withPortBindings(new ArrayList<>(allPortBindings.values()));

        // Next, ExposedPorts must be set up to publish all of the above ports, both randomized and fixed.
        createCmd.withExposedPorts(new ArrayList<>(allPortBindings.keySet()));

        if (this.command != null) {
            createCmd.withCmd(this.command);
        }

        String[] envArray = getEnvVars()
            .entrySet()
            .stream()
            .filter(it -> it.getValue() != null)
            .map(it -> it.getKey() + "=" + it.getValue())
            .toArray(String[]::new);
        createCmd.withEnv(envArray);
    }
}
