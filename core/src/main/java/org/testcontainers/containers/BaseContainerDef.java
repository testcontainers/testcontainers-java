package org.testcontainers.containers;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.SystemUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.UnstableAPI;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.ResourceReaper;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter(AccessLevel.MODULE)
@AllArgsConstructor
@UnstableAPI
@Slf4j
abstract class BaseContainerDef<S extends StartedContainer> {

    @Setter(AccessLevel.PACKAGE)
    RemoteDockerImage image;

    final Map<String, String> env = new HashMap<>();

    final Set<ExposedPort> exposedPorts = new HashSet<>();

    final Set<PortBinding> portBindings = new HashSet<>();

    @Setter(AccessLevel.PACKAGE)
    StartupCheckStrategy startupCheckStrategy = new IsRunningStartupCheckStrategy();

    @Setter(AccessLevel.PACKAGE)
    WaitStrategy waitStrategy = Wait.defaultWaitStrategy();

    @Setter(AccessLevel.PACKAGE)
    String[] command = null;

    @Setter(AccessLevel.PACKAGE)
    Network network = null;

    @Setter(AccessLevel.PACKAGE)
    String networkMode = null;

    @NonNull
    final List<Bind> binds = new ArrayList<>();

    @NonNull
    final Set<String> networkAliases = new HashSet<>(Arrays.asList(
        "tc-" + Base58.randomString(8)
    ));

    final Map<MountableFile, String> copyToFileContainerPaths = new HashMap<>();

    public BaseContainerDef(RemoteDockerImage image) {
        this.image = image;
        configure();
    }

    protected void configure() {

    }

    protected void setCommand(String... command) {
        this.command = command;
    }

    protected void setEnv(@NonNull String key, String value) {
        env.put(key, value);
    }

    protected void setEnv(Map<String, String> env) {
        this.env.clear();
        this.env.putAll(env);
    }

    protected void setExposedPorts(Set<ExposedPort> exposedPorts) {
        this.exposedPorts.clear();
        this.exposedPorts.addAll(exposedPorts);
    }

    protected void setPortBindings(Set<PortBinding> portBindings) {
        this.portBindings.clear();
        this.portBindings.addAll(portBindings);
    }

    protected void setBinds(List<Bind> binds) {
        this.binds.clear();
        this.binds.addAll(binds);
    }

    protected void addExposedPort(int port) {
        addExposedPort(port, InternetProtocol.TCP);
    }

    protected void addExposedPort(int port, @NonNull InternetProtocol protocol) {
        exposedPorts.add(toExposedPort(port, protocol));
    }

    private ExposedPort toExposedPort(int port, InternetProtocol protocol) {
        return new ExposedPort(
            port,
            com.github.dockerjava.api.model.InternetProtocol.parse(protocol.name())
        );
    }

    protected void addPortBinding(int hostPort, int containerPort, InternetProtocol protocol) {
        portBindings.add(
            new PortBinding(
                Ports.Binding.bindPort(hostPort),
                toExposedPort(containerPort, protocol)
            )
        );
    }

    protected void addNetworkAlias(String alias) {
        networkAliases.add(alias);
    }

    protected void addFileSystemBind(final String hostPath, final String containerPath, final BindMode mode) {
        addFileSystemBind(hostPath, containerPath, mode, SelinuxContext.NONE);
    }

    protected void addFileSystemBind(String hostPath, String containerPath, BindMode mode, SelinuxContext selinuxContext) {
        Bind bind;
        if (SystemUtils.IS_OS_WINDOWS && hostPath.startsWith("/")) {
            // e.g. Docker socket mount
            bind = new Bind(hostPath, new Volume(containerPath), mode.accessMode, selinuxContext.selContext);
        } else {
            final MountableFile mountableFile = MountableFile.forHostPath(hostPath);
            bind = new Bind(mountableFile.getResolvedPath(), new Volume(containerPath), mode.accessMode, selinuxContext.selContext);
        }
        binds.add(bind);
    }

    protected void addClasspathResourceMapping(final String resourcePath, final String containerPath, final BindMode mode) {
        addClasspathResourceMapping(resourcePath, containerPath, mode, SelinuxContext.NONE);
    }

    protected void addClasspathResourceMapping(String resourcePath, String containerPath, BindMode mode, SelinuxContext selinuxContext) {
        final MountableFile mountableFile = MountableFile.forClasspathResource(resourcePath);

        if (mode == BindMode.READ_ONLY && selinuxContext == SelinuxContext.NONE) {
            withCopyFileToContainer(mountableFile, containerPath);
        } else {
            addFileSystemBind(mountableFile.getResolvedPath(), containerPath, mode, selinuxContext);
        }
    }

    protected void withCopyFileToContainer(MountableFile mountableFile, String containerPath) {
        copyToFileContainerPaths.put(mountableFile, containerPath);
    }

    protected void applyTo(CreateContainerCmd createCommand) {
        HostConfig hostConfig = createCommand.getHostConfig();
        if (hostConfig == null) {
            hostConfig = new HostConfig();
            createCommand.withHostConfig(hostConfig);
        }

        // PortBindings must contain:
        //  * all exposed ports with a randomized host port (equivalent to -p CONTAINER_PORT)
        //  * all exposed ports with a fixed host port (equivalent to -p HOST_PORT:CONTAINER_PORT)
        Map<ExposedPort, PortBinding> allPortBindings = new HashMap<>();
        // First collect all the randomized host ports from our 'exposedPorts' field
        for (ExposedPort exposedPort : exposedPorts) {
            allPortBindings.put(exposedPort, new PortBinding(Ports.Binding.empty(), exposedPort));
        }
        // Next collect all the fixed host ports from our 'portBindings' field, overwriting any randomized ports so that
        // we don't create two bindings for the same container port.
        for (PortBinding portBinding : portBindings) {
            allPortBindings.put(portBinding.getExposedPort(), portBinding);
        }
        hostConfig.withPortBindings(new ArrayList<>(allPortBindings.values()));

        // Next, ExposedPorts must be set up to publish all of the above ports, both randomized and fixed.
        createCommand.withExposedPorts(new ArrayList<>(allPortBindings.keySet()));

        createCommand.withEnv(
            env.entrySet().stream()
                .filter(it -> it.getValue() != null)
                .map(it -> it.getKey() + "=" + it.getValue())
                .toArray(String[]::new)
        );

        if (this.command != null) {
            createCommand.withCmd(this.command);
        }

        if (network != null) {
            hostConfig.withNetworkMode(network.getId());
            createCommand.withAliases(networkAliases.toArray(new String[0]));
        } else {
            if (networkMode != null) {
                createCommand.withNetworkMode(networkMode);
            }
        }

        boolean shouldCheckFileMountingSupport = binds.size() > 0 && !TestcontainersConfiguration.getInstance().isDisableChecks();
        if (shouldCheckFileMountingSupport) {
            if (!DockerClientFactory.instance().isFileMountingSupported()) {
                log.warn(
                    "Unable to mount a file from test host into a running container. " +
                        "This may be a misconfiguration or limitation of your Docker environment. " +
                        "Some features might not work."
                );
            }
        }

        hostConfig.withBinds(binds.toArray(new Bind[0]));
    }

    protected abstract S toStarted(ContainerState container);

    public static class Started implements StartedContainer {

        @Delegate(types = ContainerState.class)
        protected final ContainerState containerState;

        public Started(ContainerState containerState) {
            this.containerState = containerState;
        }

        @Override
        public void close() {
            if (containerState instanceof GenericContainer) {
                ((GenericContainer<?>) containerState).stop();
                return;
            }

            if (this instanceof ContainerHooksAware) {
                ((ContainerHooksAware) this).containerIsStopping();
            }
            ResourceReaper.instance().stopAndRemoveContainer(getContainerId());
            if (this instanceof ContainerHooksAware) {
                ((ContainerHooksAware) this).containerIsStopped();
            }
        }
    }
}
