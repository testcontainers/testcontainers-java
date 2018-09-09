package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Info;
import lombok.NonNull;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.LogUtils;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.ThrowingFunction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Container<SELF extends Container<SELF>> extends LinkableContainer, ContainerState {

    /**
     * @return a reference to this container instance, cast to the expected generic type.
     */
    @SuppressWarnings("unchecked")
    default SELF self() {
        return (SELF) this;
    }

    /**
     * Class to hold results from a "docker exec" command. Note that, due to the limitations of the
     * docker API, there's no easy way to get the result code from the process we ran.
     */
    class ExecResult {
        private final String stdout;
        private final String stderr;

        public ExecResult(String stdout, String stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }
    }

    /**
     * Set the command that should be run in the container. Consider using {@link #withCommand(String)}
     * for building a container in a fluent style.
     *
     * @param command a command in single string format (will automatically be split on spaces)
     */
    void setCommand(@NonNull String command);

    /**
     * Set the command that should be run in the container. Consider using {@link #withCommand(String...)}
     * for building a container in a fluent style.
     *
     * @param commandParts a command as an array of string parts
     */
    void setCommand(@NonNull String... commandParts);

    /**
     * Add an environment variable to be passed to the container. Consider using {@link #withEnv(String, String)}
     * for building a container in a fluent style.
     *
     * @param key   environment variable key
     * @param value environment variable value
     */
    void addEnv(String key, String value);

    /**
     * Adds a file system binding. Consider using {@link #withFileSystemBind(String, String, BindMode)}
     * for building a container in a fluent style.
     *
     * @param hostPath      the file system path on the host
     * @param containerPath the file system path inside the container
     * @param mode          the bind mode
     */
    default void addFileSystemBind(final String hostPath, final String containerPath, final BindMode mode) {
        addFileSystemBind(hostPath, containerPath, mode, SelinuxContext.NONE);
    }

    /**
     * Adds a file system binding. Consider using {@link #withFileSystemBind(String, String, BindMode)}
     * for building a container in a fluent style.
     *
     * @param hostPath      the file system path on the host
     * @param containerPath the file system path inside the container
     * @param mode          the bind mode
     * @param selinuxContext selinux context argument to use for this file
     */
    void addFileSystemBind(String hostPath, String containerPath, BindMode mode, SelinuxContext selinuxContext);

    /**
     * Add a link to another container.
     *
     * @param otherContainer the other container object to link to
     * @param alias the alias (for the other container) that this container should be able to use
     * @deprecated Links are deprecated (see <a href="https://github.com/testcontainers/testcontainers-java/issues/465">#465</a>). Please use {@link Network} features instead.
     */
    @Deprecated
    void addLink(LinkableContainer otherContainer, String alias);

    /**
     * Add an exposed port. Consider using {@link #withExposedPorts(Integer...)}
     * for building a container in a fluent style.
     *
     * @param port a TCP port
     */
    void addExposedPort(Integer port);

    /**
     * Add exposed ports. Consider using {@link #withExposedPorts(Integer...)}
     * for building a container in a fluent style.
     *
     * @param ports an array of TCP ports
     */
    void addExposedPorts(int... ports);

    /**
     * Specify the {@link WaitStrategy} to use to determine if the container is ready.
     *
     * @see org.testcontainers.containers.wait.strategy.Wait#defaultWaitStrategy()
     * @param waitStrategy the WaitStrategy to use
     * @return this
     */
    SELF waitingFor(@NonNull WaitStrategy waitStrategy);

    /**
     * Adds a file system binding.
     *
     * @param hostPath the file system path on the host
     * @param containerPath the file system path inside the container
     * @return this
     */
    default SELF withFileSystemBind(String hostPath, String containerPath) {
        return withFileSystemBind(hostPath, containerPath, BindMode.READ_WRITE);
    }

    /**
     * Adds a file system binding.
     *
     * @param hostPath the file system path on the host
     * @param containerPath the file system path inside the container
     * @param mode the bind mode
     * @return this
     */
    SELF withFileSystemBind(String hostPath, String containerPath, BindMode mode);

    /**
     * Adds container volumes.
     *
     * @param container the container to add volumes from
     * @param mode the bind mode
     * @return this
     */
    SELF withVolumesFrom(Container container, BindMode mode);

    /**
     * Set the ports that this container listens on
     *
     * @param ports an array of TCP ports
     * @return this
     */
    SELF withExposedPorts(Integer... ports);

    /**
     * Set the file to be copied before starting a created container
     *
     * @param mountableFile a Mountable file with path of source file / folder on host machine
     * @param containerPath a destination path on conatiner to which the files / folders to be copied
     * @return this
     */
    SELF withCopyFileToContainer(MountableFile mountableFile, String containerPath);

    /**
     * Add an environment variable to be passed to the container.
     *
     * @param key   environment variable key
     * @param value environment variable value
     * @return this
     */
    SELF withEnv(String key, String value);

    /**
     * Add an environment variable to be passed to the container.
     *
     * @param key   environment variable key
     * @param mapper environment variable value mapper, accepts old value as an argument
     * @return this
     */
    default SELF withEnv(String key, Function<Optional<String>, String> mapper) {
        Optional<String> oldValue = Optional.ofNullable(getEnvMap().get(key));
        return withEnv(key, mapper.apply(oldValue));
    }

    /**
     * Add environment variables to be passed to the container.
     *
     * @param env map of environment variables
     * @return this
     */
    SELF withEnv(Map<String, String> env);

    /**
     * Add a label to the container.
     *
     * @param key   label key
     * @param value label value
     * @return this
     */
    SELF withLabel(String key, String value);

    /**
     * Add labels to the container.
     * @param labels map of labels
     * @return this
     */
    SELF withLabels(Map<String, String> labels);

    /**
     * Set the command that should be run in the container
     *
     * @param cmd a command in single string format (will automatically be split on spaces)
     * @return this
     */
    SELF withCommand(String cmd);

    /**
     * Set the command that should be run in the container
     *
     * @param commandParts a command as an array of string parts
     * @return this
     */
    SELF withCommand(String... commandParts);

    /**
     * Add an extra host entry to be passed to the container
     * @param hostname hostname to use for this hosts file entry
     * @param ipAddress IP address to use for this hosts file entry
     * @return this
     */
    SELF withExtraHost(String hostname, String ipAddress);

    /**
     * Set the network mode for this container, similar to the <code>--net &lt;name&gt;</code>
     * option on the docker CLI.
     *
     * @param networkMode network mode, e.g. including 'host', 'bridge', 'none' or the name of an existing named network.
     * @return this
     */
    SELF withNetworkMode(String networkMode);

    /**
     * Set the network for this container, similar to the <code>--network &lt;name&gt;</code>
     * option on the docker CLI.
     *
     * @param network the instance of {@link Network}
     * @return this
     */
    SELF withNetwork(Network network);

    /**
     * Set the network aliases for this container, similar to the <code>--network-alias &lt;my-service&gt;</code>
     * option on the docker CLI.
     *
     * @param aliases the list of aliases
     * @return this
     */
    SELF withNetworkAliases(String... aliases);

    /**
     * Map a resource (file or directory) on the classpath to a path inside the container.
     * This will only work if you are running your tests outside a Docker container.
     *
     * @param resourcePath  path to the resource on the classpath (relative to the classpath root; should not start with a leading slash)
     * @param containerPath path this should be mapped to inside the container
     * @param mode          access mode for the file
     * @return this
     */
    default SELF withClasspathResourceMapping(final String resourcePath, final String containerPath, final BindMode mode) {
        withClasspathResourceMapping(resourcePath, containerPath, mode, SelinuxContext.NONE);
        return self();
    }

    /**
     * Map a resource (file or directory) on the classpath to a path inside the container.
     * This will only work if you are running your tests outside a Docker container.
     *
     * @param resourcePath   path to the resource on the classpath (relative to the classpath root; should not start with a leading slash)
     * @param containerPath  path this should be mapped to inside the container
     * @param mode           access mode for the file
     * @param selinuxContext selinux context argument to use for this file
     * @return this
     */
    SELF withClasspathResourceMapping(String resourcePath, String containerPath, BindMode mode, SelinuxContext selinuxContext);

    /**
     * Set the duration of waiting time until container treated as started.
     * @see WaitStrategy#waitUntilReady(org.testcontainers.containers.wait.strategy.WaitStrategyTarget)
     *
     * @param startupTimeout timeout
     * @return this
     */
    SELF withStartupTimeout(Duration startupTimeout);

    /**
     * Set the privilegedMode mode for the container
     * @param mode boolean
     * @return this
     */
    SELF withPrivilegedMode(boolean mode);

    /**
     * Only consider a container to have successfully started if it has been running for this duration. The default
     * value is null; if that's the value, ignore this check.
     *
     * @param minimumRunningDuration duration this container should run for if started successfully
     * @return this
     */
    SELF withMinimumRunningDuration(Duration minimumRunningDuration);

    /**
     * Set the startup check strategy used for checking whether the container has started.
     *
     * @param strategy startup check strategy
     */
    SELF withStartupCheckStrategy(StartupCheckStrategy strategy);

    /**
     * Set the working directory that the container should use on startup.
     *
     * @param workDir path to the working directory inside the container
     */
    SELF withWorkingDirectory(String workDir);

    /**
     * <b>Resolve</b> Docker image and set it.
     *
     * @param dockerImageName image name
     */
    void setDockerImageName(@NonNull String dockerImageName);

    /**
     * Get image name.
     *
     * @return image name
     */
    @NonNull
    String getDockerImageName();

    /**
     * Get the IP address that containers (e.g. browsers) can use to reference a service running on the local machine,
     * i.e. the machine on which this test is running.
     * <p>
     * For example, if a web server is running on port 8080 on this local machine, the containerized web driver needs
     * to be pointed at "http://" + getTestHostIpAddress() + ":8080" in order to access it. Trying to hit localhost
     * from inside the container is not going to work, since the container has its own IP address.
     *
     * @return the IP address of the host machine
     */
    String getTestHostIpAddress();

    /**
     * Follow container output, sending each frame (usually, line) to a consumer. Stdout and stderr will be followed.
     *
     * @param consumer consumer that the frames should be sent to
     */
    default void followOutput(Consumer<OutputFrame> consumer) {
        LogUtils.followOutput(DockerClientFactory.instance().client(), getContainerId(), consumer);
    }

    /**
     * Follow container output, sending each frame (usually, line) to a consumer. This method allows Stdout and/or stderr
     * to be selected.
     *
     * @param consumer consumer that the frames should be sent to
     * @param types    types that should be followed (one or both of STDOUT, STDERR)
     */
    default void followOutput(Consumer<OutputFrame> consumer, OutputFrame.OutputType... types) {
        LogUtils.followOutput(DockerClientFactory.instance().client(), getContainerId(), consumer, types);
    }


    /**
     * Attach an output consumer at container startup, enabling stdout and stderr to be followed, waited on, etc.
     * <p>
     * More than one consumer may be registered.
     *
     * @param consumer consumer that output frames should be sent to
     * @return this
     */
    SELF withLogConsumer(Consumer<OutputFrame> consumer);

    /**
     *
     * @deprecated please use {@code org.testcontainers.DockerClientFactory.instance().client().infoCmd().exec()}
     */
    @Deprecated
    Info fetchDockerDaemonInfo() throws IOException;

    /**
     * Run a command inside a running container, as though using "docker exec", and interpreting
     * the output as UTF8.
     * <p>
     * @see ExecInContainerPattern#execInContainer(com.github.dockerjava.api.command.InspectContainerResponse, String...)
     */
    ExecResult execInContainer(String... command)
            throws UnsupportedOperationException, IOException, InterruptedException;

    /**
     * Run a command inside a running container, as though using "docker exec".
     * <p>
     * @see ExecInContainerPattern#execInContainer(com.github.dockerjava.api.command.InspectContainerResponse, Charset, String...)
     */
    ExecResult execInContainer(Charset outputCharset, String... command)
                    throws UnsupportedOperationException, IOException, InterruptedException;

    /**
     *
     * Copies a file or directory to the container.
     *
     * @param mountableFile file or directory which is copied into the container
     * @param containerPath destination path inside the container
     * @throws IOException if there's an issue communicating with Docker
     * @throws InterruptedException if the thread waiting for the response is interrupted
     */
    void copyFileToContainer(MountableFile mountableFile, String containerPath) throws IOException, InterruptedException;

    /**
     *
     * Copies a file to the container.
     *
     * @param transferable file which is copied into the container
     * @param containerPath destination path inside the container
     */
    void copyFileToContainer(Transferable transferable, String containerPath);

    /**
     * Copies a file which resides inside the container to user defined directory
     *
     * @param containerPath path to file which is copied from container
     * @param destinationPath destination path to which file is copied with file name
     * @throws IOException if there's an issue communicating with Docker or receiving entry from TarArchiveInputStream
     * @throws InterruptedException if the thread waiting for the response is interrupted
     */
    void copyFileFromContainer(String containerPath, String destinationPath) throws IOException, InterruptedException;

    /**
     * Streams a file which resides inside the container
     *
     * @param containerPath path to file which is copied from container
     * @param function function that takes InputStream of the copied file
     */
    <T> T copyFileFromContainer(String containerPath, ThrowingFunction<InputStream, T> function);

    List<String> getPortBindings();

    List<String> getExtraHosts();

    Future<String> getImage();

    /**
     *
     * @deprecated use getEnvMap
     */
    @Deprecated
    List<String> getEnv();

    Map<String, String> getEnvMap();

    String[] getCommandParts();

    List<Bind> getBinds();

    /**
     * @deprecated Links are deprecated (see <a href="https://github.com/testcontainers/testcontainers-java/issues/465">#465</a>). Please use {@link Network} features instead.
     */
    @Deprecated
    Map<String, LinkableContainer> getLinkedContainers();

    DockerClient getDockerClient();

    /**
     *
     * @deprecated please use {@code org.testcontainers.DockerClientFactory.instance().client().infoCmd().exec()}
     */
    @Deprecated
    Info getDockerDaemonInfo();

    void setExposedPorts(List<Integer> exposedPorts);

    void setPortBindings(List<String> portBindings);

    void setExtraHosts(List<String> extraHosts);

    void setImage(Future<String> image);

    void setEnv(List<String> env);

    void setCommandParts(String[] commandParts);

    void setBinds(List<Bind> binds);

    /**
     * @deprecated Links are deprecated (see <a href="https://github.com/testcontainers/testcontainers-java/issues/465">#465</a>). Please use {@link Network} features instead.
     */
    @Deprecated
    void setLinkedContainers(Map<String, LinkableContainer> linkedContainers);

    void setWaitStrategy(WaitStrategy waitStrategy);
}
