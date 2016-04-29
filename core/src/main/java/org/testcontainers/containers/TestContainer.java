package org.testcontainers.containers;

import com.github.dockerjava.api.model.Info;
import lombok.NonNull;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.wait.WaitStrategy;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public interface TestContainer<SELF extends TestContainer<SELF>> extends LinkableContainer {

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

    SELF waitingFor(@NonNull WaitStrategy waitStrategy);

    void setCommand(@NonNull String command);

    void setCommand(@NonNull String... commandParts);

    void addEnv(String key, String value);

    void addFileSystemBind(String hostPath, String containerPath, BindMode mode);

    SELF withFileSystemBind(String hostPath, String containerPath, BindMode mode);

    void addLink(LinkableContainer otherContainer, String alias);

    void addExposedPort(Integer port);

    void addExposedPorts(int... ports);

    SELF withExposedPorts(Integer... ports);

    SELF withEnv(String key, String value);

    SELF withCommand(String cmd);

    SELF withCommand(String... commandParts);

    SELF withExtraHost(String hostname, String ipAddress);

    SELF withClasspathResourceMapping(String resourcePath, String containerPath, BindMode mode);

    SELF withStartupTimeout(Duration startupTimeout);

    String getContainerIpAddress();

    SELF withMinimumRunningDuration(Duration minimumRunningDuration);

    @Deprecated
    String getIpAddress();

    Boolean isRunning();

    Integer getMappedPort(int originalPort);

    void setDockerImageName(@NonNull String dockerImageName);

    @NonNull
    String getDockerImageName();

    String getTestHostIpAddress();

    void followOutput(Consumer<OutputFrame> consumer);

    void followOutput(Consumer<OutputFrame> consumer, OutputFrame.OutputType... types);

    Info fetchDockerDaemonInfo() throws IOException;

    ExecResult execInContainer(String... command)
            throws UnsupportedOperationException, IOException, InterruptedException;

    ExecResult execInContainer(Charset outputCharset, String... command)
                    throws UnsupportedOperationException, IOException, InterruptedException;

    List<Integer> getExposedPorts();

    List<String> getPortBindings();

    List<String> getExtraHosts();

    java.util.concurrent.Future<String> getImage();

    List<String> getEnv();

    String[] getCommandParts();

    List<com.github.dockerjava.api.model.Bind> getBinds();

    java.util.Map<String, LinkableContainer> getLinkedContainers();

    Duration getMinimumRunningDuration();

    com.github.dockerjava.api.DockerClient getDockerClient();

    Info getDockerDaemonInfo();

    String getContainerId();

    String getContainerName();

    com.github.dockerjava.api.command.InspectContainerResponse getContainerInfo();

    void setExposedPorts(List<Integer> exposedPorts);

    void setPortBindings(List<String> portBindings);

    void setExtraHosts(List<String> extraHosts);

    void setImage(java.util.concurrent.Future<String> image);

    void setEnv(List<String> env);

    void setCommandParts(String[] commandParts);

    void setBinds(List<com.github.dockerjava.api.model.Bind> binds);

    void setLinkedContainers(java.util.Map<String, LinkableContainer> linkedContainers);

    void setMinimumRunningDuration(Duration minimumRunningDuration);

    void setDockerClient(com.github.dockerjava.api.DockerClient dockerClient);

    void setDockerDaemonInfo(Info dockerDaemonInfo);

    void setContainerId(String containerId);

    void setContainerName(String containerName);

    void setWaitStrategy(WaitStrategy waitStrategy);

    void setContainerInfo(com.github.dockerjava.api.command.InspectContainerResponse containerInfo);
}
