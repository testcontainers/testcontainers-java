package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.*;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.testcontainers.ContainerState;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.utility.DockerLoggerFactory;
import org.testcontainers.utility.DockerMachineClient;
import org.testcontainers.utility.LogUtils;
import org.testcontainers.utility.TestEnvironment;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.testcontainers.utility.CommandLine.runShellCommand;
@Getter
public abstract class AbstractContainerState implements ContainerState {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    /*
     * Default settings
     */
    @NonNull
    protected List<Integer> exposedPorts = new ArrayList<>();

    @NonNull
    protected List<String> portBindings = new ArrayList<>();

    @NonNull
    protected List<String> extraHosts = new ArrayList<>();

    @NonNull
    protected Future<String> image;

    @Getter(AccessLevel.NONE)
    @NonNull
    protected Map<String, String> env = new HashMap<>();

    @NonNull
    protected String[] commandParts = new String[0];

    @NonNull
    protected List<Bind> binds = new ArrayList<>();

    /**
     * @deprecated Links are deprecated (see <a href="https://github.com/testcontainers/testcontainers-java/issues/465">#465</a>). Please use {@link Network} features instead.
     */
    @NonNull
    @Deprecated
    protected Map<String, LinkableContainer> linkedContainers = new HashMap<>();

    /*
     * Unique instance of DockerClient for use by this container object.
     */
    @Setter(AccessLevel.NONE)
    protected DockerClient dockerClient = DockerClientFactory.instance().client();
    protected Info dockerDaemonInfo = null;

    /*
     * Set during container startup
     */
    @Setter(AccessLevel.NONE)
    protected String containerId;

    @Setter(AccessLevel.NONE)
    protected String containerName;

    @Nullable
    @Setter(AccessLevel.PROTECTED)
    protected InspectContainerResponse containerInfo;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContainerIpAddress() {
        return DockerClientFactory.instance().dockerHostIpAddress();
    }


    @Override
    public Boolean isRunning() {
        try {
            String containerId = getContainerId();
            return containerId != null && dockerClient.inspectContainerCmd(containerId).exec().getState().getRunning();
        } catch (DockerException e) {
            return false;
        }
    }

    @Override
    public String getDockerImageName() {
        Future<String> image = getImage();
        try {
            return image.get();
        } catch (Exception e) {
            throw new ContainerFetchException("Can't get Docker image name from " + image, e);
        }
    }

    @Override
    public String getTestHostIpAddress() {
        if (DockerMachineClient.instance().isInstalled()) {
            try {
                Optional<String> defaultMachine = DockerMachineClient.instance().getDefaultMachine();
                if (!defaultMachine.isPresent()) {
                    throw new IllegalStateException("Could not find a default docker-machine instance");
                }

                String sshConnectionString = runShellCommand("docker-machine", "ssh", defaultMachine.get(), "echo $SSH_CONNECTION").trim();
                if (Strings.isNullOrEmpty(sshConnectionString)) {
                    throw new IllegalStateException("Could not obtain SSH_CONNECTION environment variable for docker machine " + defaultMachine.get());
                }

                String[] sshConnectionParts = sshConnectionString.split("\\s");
                if (sshConnectionParts.length != 4) {
                    throw new IllegalStateException("Unexpected pattern for SSH_CONNECTION for docker machine - expected 'IP PORT IP PORT' pattern but found '" + sshConnectionString + "'");
                }

                return sshConnectionParts[0];
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else {
            throw new UnsupportedOperationException("getTestHostIpAddress() is only implemented for docker-machine right now");
        }
    }

    @Override
    public Info fetchDockerDaemonInfo() throws IOException {
        if (this.dockerDaemonInfo == null) {
            this.dockerDaemonInfo = this.dockerClient.infoCmd().exec();
        }
        return this.dockerDaemonInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getEnv() {
        return env.entrySet().stream()
            .map(it -> it.getKey() + "=" + it.getValue())
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getEnvMap() {
        return env;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getMappedPort(final int originalPort) {

        Preconditions.checkState(containerId != null, "Mapped port can only be obtained after the container is started");

        Ports.Binding[] binding = new Ports.Binding[0];
        if (containerInfo != null) {
            binding = containerInfo.getNetworkSettings().getPorts().getBindings().get(new ExposedPort(originalPort));
        }

        if (binding != null && binding.length > 0 && binding[0] != null) {
            return Integer.valueOf(binding[0].getHostPortSpec());
        } else {
            throw new IllegalArgumentException("Requested port (" + originalPort + ") is not mapped");
        }
    }

    @NotNull
    @NonNull
    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        final Set<Integer> result = new LinkedHashSet<>();
        result.addAll(getExposedPortNumbers());
        result.addAll(getBoundPortNumbers());

        return result;
    }

    private List<Integer> getExposedPortNumbers() {
        return exposedPorts.stream()
            .map(this::getMappedPort)
            .collect(Collectors.toList());
    }

    private List<Integer> getBoundPortNumbers() {
        return portBindings.stream()
            .map(PortBinding::parse)
            .map(PortBinding::getBinding)
            .map(Ports.Binding::getHostPortSpec)
            .map(Integer::valueOf)
            .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void followOutput(Consumer<OutputFrame> consumer) {
        this.followOutput(consumer, OutputFrame.OutputType.STDOUT, OutputFrame.OutputType.STDERR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void followOutput(Consumer<OutputFrame> consumer, OutputFrame.OutputType... types) {
        LogUtils.followOutput(dockerClient, containerId, consumer, types);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Container.ExecResult execInContainer(String... command)
        throws UnsupportedOperationException, IOException, InterruptedException {

        return execInContainer(UTF8, command);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Container.ExecResult execInContainer(Charset outputCharset, String... command)
        throws UnsupportedOperationException, IOException, InterruptedException {

        if (!TestEnvironment.dockerExecutionDriverSupportsExec()) {
            // at time of writing, this is the expected result in CircleCI.
            throw new UnsupportedOperationException(
                "Your docker daemon is running the \"lxc\" driver, which doesn't support \"docker exec\".");

        }

        if (!isRunning()) {
            throw new IllegalStateException("execInContainer can only be used while the Container is running");
        }

        this.dockerClient
            .execCreateCmd(this.containerId)
            .withCmd(command);

        logger().debug("Running \"exec\" command: " + String.join(" ", command));
        final ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(this.containerId)
            .withAttachStdout(true).withAttachStderr(true).withCmd(command).exec();

        final ToStringConsumer stdoutConsumer = new ToStringConsumer();
        final ToStringConsumer stderrConsumer = new ToStringConsumer();

        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        callback.addConsumer(OutputFrame.OutputType.STDOUT, stdoutConsumer);
        callback.addConsumer(OutputFrame.OutputType.STDERR, stderrConsumer);

        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback).awaitCompletion();

        final Container.ExecResult result = new Container.ExecResult(
            stdoutConsumer.toString(outputCharset),
            stderrConsumer.toString(outputCharset));

        logger().trace("stdout: " + result.getStdout());
        logger().trace("stderr: " + result.getStderr());
        return result;
    }

    /**
     * Provide a logger that references the docker image name.
     *
     * @return a logger that references the docker image name
     */
    @Override
    public Logger logger() {
        return DockerLoggerFactory.getLogger(this.getDockerImageName());
    }

}
