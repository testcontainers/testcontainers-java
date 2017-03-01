package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Info;
import lombok.NonNull;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.wait.Wait;
import org.testcontainers.containers.wait.WaitStrategy;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface Container<SELF extends Container<SELF>> extends LinkableContainer {

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
     * @param hostPath the file system path on the host
     * @param containerPath the file system path inside the container
     * @param mode the bind mode
     */
    void addFileSystemBind(String hostPath, String containerPath, BindMode mode);

    /**
     * Add a link to another container.
     *
     * @param otherContainer the other container object to link to
     * @param alias the alias (for the other container) that this container should be able to use
     */
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
     * @see Wait#defaultWaitStrategy()
     * @param waitStrategy the WaitStrategy to use
     * @return this
     */
    SELF waitingFor(@NonNull WaitStrategy waitStrategy);

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
     * Add an environment variable to be passed to the container.
     *
     * @param key   environment variable key
     * @param value environment variable value
     * @return this
     */
    SELF withEnv(String key, String value);

    /**
     * Add environment variables to be passed to the container.
     *
     * @param env map of environment variables
     * @return this
     */
    SELF withEnv(Map<String, String> env);

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
     * Map a resource (file or directory) on the classpath to a path inside the container.
     * This will only work if you are running your tests outside a Docker container.
     *
     * @param resourcePath  path to the resource on the classpath (relative to the classpath root; should not start with a leading slash)
     * @param containerPath path this should be mapped to inside the container
     * @param mode          access mode for the file
     * @return this
     */
    SELF withClasspathResourceMapping(String resourcePath, String containerPath, BindMode mode);

    /**
     * Set the duration of waiting time until container treated as started.
     * @see WaitStrategy#waitUntilReady(GenericContainer)
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
     * Get the IP address that this container may be reached on (may not be the local machine).
     *
     * @return an IP address
     */
    String getContainerIpAddress();

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
     * @return is the container currently running?
     */
    Boolean isRunning();

    /**
     * Get the actual mapped port for a given port exposed by the container.
     *
     * @param originalPort the original TCP port that is exposed
     * @return the port that the exposed port is mapped to, or null if it is not exposed
     */
    Integer getMappedPort(int originalPort);

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
    void followOutput(Consumer<OutputFrame> consumer);

    /**
     * Follow container output, sending each frame (usually, line) to a consumer. This method allows Stdout and/or stderr
     * to be selected.
     *
     * @param consumer consumer that the frames should be sent to
     * @param types    types that should be followed (one or both of STDOUT, STDERR)
     */
    void followOutput(Consumer<OutputFrame> consumer, OutputFrame.OutputType... types);


    /**
     * Attach an output consumer at container startup, enabling stdout and stderr to be followed, waited on, etc.
     * <p>
     * More than one consumer may be registered.
     *
     * @param consumer consumer that output frames should be sent to
     * @return this
     */
    SELF withLogConsumer(Consumer<OutputFrame> consumer);

    Info fetchDockerDaemonInfo() throws IOException;

    /**
     * Run a command inside a running container, as though using "docker exec", and interpreting
     * the output as UTF8.
     * <p>
     * @see #execInContainer(Charset, String...)
     */
    ExecResult execInContainer(String... command)
            throws UnsupportedOperationException, IOException, InterruptedException;

    /**
     * Run a command inside a running container, as though using "docker exec".
     * <p>
     * This functionality is not available on a docker daemon running the older "lxc" execution driver. At
     * the time of writing, CircleCI was using this driver.
     * @param outputCharset the character set used to interpret the output.
     * @param command the parts of the command to run
     * @return the result of execution
     * @throws IOException if there's an issue communicating with Docker
     * @throws InterruptedException if the thread waiting for the response is interrupted
     * @throws UnsupportedOperationException if the docker daemon you're connecting to doesn't support "exec".
     */
    ExecResult execInContainer(Charset outputCharset, String... command)
                    throws UnsupportedOperationException, IOException, InterruptedException;

    List<Integer> getExposedPorts();

    List<String> getPortBindings();

    List<String> getExtraHosts();

    Future<String> getImage();

    List<String> getEnv();

    String[] getCommandParts();

    List<Bind> getBinds();

    Map<String, LinkableContainer> getLinkedContainers();

    DockerClient getDockerClient();

    Info getDockerDaemonInfo();

    String getContainerId();

    String getContainerName();

    InspectContainerResponse getContainerInfo();

    void setExposedPorts(List<Integer> exposedPorts);

    void setPortBindings(List<String> portBindings);

    void setExtraHosts(List<String> extraHosts);

    void setImage(Future<String> image);

    void setEnv(List<String> env);

    void setCommandParts(String[] commandParts);

    void setBinds(List<Bind> binds);

    void setLinkedContainers(Map<String, LinkableContainer> linkedContainers);

    /**
     * @deprecated set by GenericContainer and should never be set outside
     */
    @Deprecated
    void setDockerClient(DockerClient dockerClient);

    /**
     * @deprecated set by GenericContainer and should never be set outside
     */
    @Deprecated
    void setDockerDaemonInfo(Info dockerDaemonInfo);

    /**
     * @deprecated set by GenericContainer and should never be set outside
     */
    @Deprecated
    void setContainerId(String containerId);

    /**
     * @deprecated set by GenericContainer and should never be set outside
     */
    @Deprecated
    void setContainerName(String containerName);

    void setWaitStrategy(WaitStrategy waitStrategy);

    /**
     * @deprecated set by GenericContainer and should never be set outside
     */
    @Deprecated
    void setContainerInfo(InspectContainerResponse containerInfo);
}
