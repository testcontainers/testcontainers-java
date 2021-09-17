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
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.SystemUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter// (AccessLevel.PROTECTED)
@Setter(AccessLevel.PACKAGE)
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@UnstableAPI
@Slf4j
abstract class BaseContainerDef<S extends StartedContainer> {

    private RemoteDockerImage image;

    private ImmutableMap<String, String> env = Maps.immutable.empty();

    private ImmutableSet<ExposedPort> exposedPorts = Sets.immutable.empty();

    private ImmutableSet<PortBinding> portBindings = Sets.immutable.empty();

    private StartupCheckStrategy startupCheckStrategy = new IsRunningStartupCheckStrategy();

    private WaitStrategy waitStrategy = Wait.defaultWaitStrategy();

    private String[] command = null;

    private Network network = null;

    private String networkMode = null;

    @NonNull
    private ImmutableList<Bind> binds = Lists.immutable.empty();

    @NonNull
    private ImmutableSet<String> networkAliases = Sets.immutable.of(
        "tc-" + Base58.randomString(8)
    );

    private ImmutableMap<MountableFile, String> copyToFileContainerPaths = Maps.immutable.empty();

    public BaseContainerDef(RemoteDockerImage image) {
        this.image = image;
        configure();
    }

    protected void configure() {

    }

    public Map<String, String> getEnv() {
        return env.castToMap();
    }

    public Set<ExposedPort> getExposedPorts() {
        return exposedPorts.castToSet();
    }

    public Set<PortBinding> getPortBindings() {
        return portBindings.castToSet();
    }

    public List<Bind> getBinds() {
        return binds.castToList();
    }

    public Set<String> getNetworkAliases() {
        return networkAliases.castToSet();
    }

    public Map<MountableFile, String> getCopyToFileContainerPaths() {
        return copyToFileContainerPaths.castToMap();
    }

    protected void setCommand(String... command) {
        this.command = command;
    }

    protected void setEnv(@NonNull String key, String value) {
        env = env.newWithKeyValue(key, value);
    }

    protected void setEnv(Map<String, String> env) {
        this.env = Maps.immutable.ofMap(env);
    }

    protected void setExposedPorts(Set<ExposedPort> exposedPorts) {
        this.exposedPorts = Sets.immutable.ofAll(exposedPorts);
    }

    protected void setPortBindings(Set<PortBinding> portBindings) {
        this.portBindings = Sets.immutable.ofAll(portBindings);
    }

    protected void setBinds(List<Bind> binds) {
        this.binds = Lists.immutable.ofAll(binds);
    }

    protected void addExposedPort(int port) {
        addExposedPort(port, InternetProtocol.TCP);
    }

    protected void addExposedPort(int port, @NonNull InternetProtocol protocol) {
        exposedPorts = exposedPorts.newWith(toExposedPort(port, protocol));
    }

    private ExposedPort toExposedPort(int port, InternetProtocol protocol) {
        return new ExposedPort(
            port,
            com.github.dockerjava.api.model.InternetProtocol.parse(protocol.name())
        );
    }

    protected void addPortBinding(int hostPort, int containerPort, InternetProtocol protocol) {
        portBindings = portBindings.newWith(
            new PortBinding(
                Ports.Binding.bindPort(hostPort),
                toExposedPort(containerPort, protocol)
            )
        );
    }

    protected void addNetworkAlias(String alias) {
        networkAliases = networkAliases.newWith(alias);
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
        binds = binds.newWith(bind);
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
        copyToFileContainerPaths = copyToFileContainerPaths.newWithKeyValue(mountableFile, containerPath);
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
            env.keyValuesView()
                .collectIf(
                    it -> it.getTwo() != null,
                    it -> it.getOne() + "=" + it.getTwo()
                )
                .toArray(new String[0])
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
