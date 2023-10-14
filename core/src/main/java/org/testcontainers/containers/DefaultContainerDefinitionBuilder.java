package org.testcontainers.containers;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
class DefaultContainerDefinitionBuilder implements ContainerDefinition.Builder {

    private DockerImageName image;

    private Set<ExposedPort> exposedPorts = new HashSet<>();

    private Set<PortBinding> portBindings = new HashSet<>();

    private Map<String, String> labels = new HashMap<>();

    private Map<String, String> environmentVariables = new HashMap<>();

    private String[] command;

    private Network network;

    private Set<String> networkAliases = new HashSet<>();

    private String networkMode;

    private List<Bind> binds = new ArrayList<>();

    private boolean privilegedMode;

    private List<Startable> dependsOn = new ArrayList<>();

    private WaitStrategy waitStrategy;

    private Consumer<CreateContainerCmd> createContainerCmdConsumer;

    public DefaultContainerDefinitionBuilder() {}

    public DefaultContainerDefinitionBuilder(DefaultContainerDefinitionBuilder other) {
        this.image = other.image;
        this.exposedPorts = other.exposedPorts;
        this.portBindings = other.portBindings;
        this.labels = other.labels;
        this.environmentVariables = other.environmentVariables;
        this.command = other.command;
        this.network = other.network;
        this.networkAliases = other.networkAliases;
        this.networkMode = other.networkMode;
        this.binds = other.binds;
        this.privilegedMode = other.privilegedMode;
        this.dependsOn = other.dependsOn;
        this.waitStrategy = other.waitStrategy;
        this.createContainerCmdConsumer = other.createContainerCmdConsumer;
    }

    @Override
    public ContainerDefinition.Builder image(DockerImageName image) {
        this.image = image;
        return this;
    }

    @Override
    public ContainerDefinition.Builder image(String image) {
        this.image = DockerImageName.parse(image);
        return this;
    }

    @Override
    public ContainerDefinition.Builder exposePort(ExposedPort exposedPort) {
        this.exposedPorts.add(exposedPort);
        return this;
    }

    @Override
    public ContainerDefinition.Builder exposePorts(ExposedPort... exposedPorts) {
        this.exposedPorts.clear();
        this.exposedPorts.addAll(Arrays.asList(exposedPorts));
        return this;
    }

    @Override
    public ContainerDefinition.Builder exposeTcpPort(int port) {
        this.exposedPorts.add(ExposedPort.tcp(port));
        return this;
    }

    @Override
    public ContainerDefinition.Builder exposeTcpPorts(int... exposedPorts) {
        this.exposedPorts.clear();
        for (int exposedPort : exposedPorts) {
            this.exposedPorts.add(ExposedPort.tcp(exposedPort));
        }
        return this;
    }

    @Override
    public ContainerDefinition.Builder portBindings(PortBinding... portBindings) {
        this.portBindings.addAll(Arrays.asList(portBindings));
        return this;
    }

    @Override
    public ContainerDefinition.Builder label(String key, String value) {
        this.labels.put(key, value);
        return this;
    }

    @Override
    public ContainerDefinition.Builder labels(Map<String, String> labels) {
        this.labels.putAll(labels);
        return this;
    }

    @Override
    public ContainerDefinition.Builder binds(Bind... binds) {
        this.binds.addAll(Arrays.asList(binds));
        return this;
    }

    @Override
    public ContainerDefinition.Builder environmentVariable(String key, String value) {
        this.environmentVariables.put(key, value);
        return this;
    }

    @Override
    public ContainerDefinition.Builder environmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables.putAll(environmentVariables);
        return this;
    }

    @Override
    public ContainerDefinition.Builder command(String... command) {
        this.command = command;
        return this;
    }

    @Override
    public ContainerDefinition.Builder network(Network network) {
        this.network = network;
        return this;
    }

    @Override
    public ContainerDefinition.Builder networkAliases(String... aliases) {
        this.networkAliases.addAll(Arrays.asList(aliases));
        return this;
    }

    @Override
    public ContainerDefinition.Builder networkMode(String networkMode) {
        this.networkMode = networkMode;
        return this;
    }

    @Override
    public ContainerDefinition.Builder privilegedMode(boolean privilegedMode) {
        this.privilegedMode = privilegedMode;
        return this;
    }

    @Override
    public ContainerDefinition.Builder dependsOn(Startable... startables) {
        this.dependsOn.addAll(Arrays.asList(startables));
        return this;
    }

    @Override
    public ContainerDefinition.Builder waitStrategy(WaitStrategy waitStrategy) {
        this.waitStrategy = waitStrategy;
        return this;
    }

    @Override
    public ContainerDefinition.Builder createContainerCmdModifier(
        Consumer<CreateContainerCmd> createContainerCmdConsumer
    ) {
        this.createContainerCmdConsumer = createContainerCmdConsumer;
        return this;
    }

    @Override
    public ContainerDefinition.Builder apply(Consumer<ContainerDefinition.Builder> builderConsumer) {
        builderConsumer.accept(this);
        return this;
    }

    @Override
    public ContainerDefinition build() {
        Consumer<CreateContainerCmd> createContainerCmdConsumer = createContainerCmd();
        return new DefaultContainerDefinition(
            this.image,
            createContainerCmdConsumer,
            this.waitStrategy,
            new DefaultContainerDefinitionBuilder(this)
        );
    }

    private Consumer<CreateContainerCmd> createContainerCmd() {
        return new DefaultCreateContainerCmd(
            this.exposedPorts,
            this.portBindings,
            this.labels,
            this.environmentVariables,
            this.command,
            this.network,
            this.networkAliases,
            this.networkMode,
            this.binds,
            this.privilegedMode
        );
    }

    static class DefaultCreateContainerCmd implements Consumer<CreateContainerCmd> {

        private final Set<ExposedPort> exposedPorts;

        private final Set<PortBinding> portBindings;

        private final Map<String, String> labels;

        private final Map<String, String> environmentVariables;

        private final String[] command;

        private final Network network;

        private final Set<String> networkAliases;

        private final String networkMode;

        private final List<Bind> binds;

        private final boolean privilegedMode;

        public DefaultCreateContainerCmd(
            Set<ExposedPort> exposedPorts,
            Set<PortBinding> portBindings,
            Map<String, String> labels,
            Map<String, String> environmentVariables,
            String[] command,
            Network network,
            Set<String> networkAliases,
            String networkMode,
            List<Bind> binds,
            boolean privilegedMode
        ) {
            this.exposedPorts = exposedPorts;
            this.portBindings = portBindings;
            this.labels = labels;
            this.environmentVariables = environmentVariables;
            this.command = command;
            this.network = network;
            this.networkAliases = networkAliases;
            this.networkMode = networkMode;
            this.binds = binds;
            this.privilegedMode = privilegedMode;
        }

        @Override
        public void accept(CreateContainerCmd createCommand) {
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
}
