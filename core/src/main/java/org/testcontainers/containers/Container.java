package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Info;
import lombok.NonNull;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.traits.*;
import org.testcontainers.containers.wait.Wait;
import org.testcontainers.containers.wait.WaitStrategy;
import org.testcontainers.utility.SelfReference;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface Container<SELF extends Container<SELF>> extends LinkableContainer, SelfReference<SELF>, TraitsSupport<SELF>,
        // old commands like `withExposedPorts` are supported with Support interfaces
        Command.Support<SELF>,
        FileSystemBind.Support<SELF>,
        ClasspathBind.Support<SELF>,
        ExposedPort.Support<SELF>,
        PortBinding.Support<SELF>,
        Env.Support<SELF>,
        ExtraHost.Support<SELF>,
        Link.Support<SELF> {

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
     * Specify the {@link WaitStrategy} to use to determine if the container is ready.
     *
     * @see Wait#defaultWaitStrategy()
     * @param waitStrategy the WaitStrategy to use
     * @return this
     */
    SELF waitingFor(@NonNull WaitStrategy waitStrategy);

    /**
     * Set the duration of waiting time until container treated as started.
     * @see WaitStrategy#waitUntilReady(GenericContainer)
     *
     * @param startupTimeout timeout
     * @return this
     */
    SELF withStartupTimeout(Duration startupTimeout);

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

    Future<String> getImage();

    Duration getMinimumRunningDuration();

    DockerClient getDockerClient();

    Info getDockerDaemonInfo();

    String getContainerId();

    String getContainerName();

    InspectContainerResponse getContainerInfo();

    void setImage(Future<String> image);

    void setMinimumRunningDuration(Duration minimumRunningDuration);

    void setDockerClient(DockerClient dockerClient);

    void setDockerDaemonInfo(Info dockerDaemonInfo);

    void setContainerId(String containerId);

    void setContainerName(String containerName);

    void setWaitStrategy(WaitStrategy waitStrategy);

    void setContainerInfo(InspectContainerResponse containerInfo);
}
