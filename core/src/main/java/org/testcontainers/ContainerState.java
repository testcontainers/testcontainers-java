package org.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Info;
import lombok.NonNull;
import org.slf4j.Logger;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.traits.LinkableContainer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface ContainerState {

    /**
     * Get the IP address that this container may be reached on (may not be the local machine).
     *
     * @return an IP address
     */
    String getContainerIpAddress();

    /**
     * @return is the container currently running?
     */
    Boolean isRunning();

    /**
     * Get the actual mapped port for a first port exposed by the container.
     *
     * @return the port that the exposed port is mapped to
     * @throws IllegalStateException if there are no exposed ports
     */
    default Integer getFirstMappedPort() {
        return getExposedPorts()
            .stream()
            .findFirst()
            .map(this::getMappedPort)
            .orElseThrow(() -> new IllegalStateException("Container doesn't expose any ports"));
    }

    /**
     * Get the actual mapped port for a given port exposed by the container.
     *
     * @param originalPort the original TCP port that is exposed
     * @return the port that the exposed port is mapped to, or null if it is not exposed
     */
    Integer getMappedPort(int originalPort);

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
     *
     * @deprecated please use {@code org.testcontainers.DockerClientFactory.instance().client().infoCmd().exec()}
     */
    @Deprecated
    Info fetchDockerDaemonInfo() throws IOException;

    List<Integer> getExposedPorts();

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

    String getContainerId();

    String getContainerName();

    /**
     *
     * @deprecated please use {@code org.testcontainers.DockerClientFactory.instance().client().inspectContainerCmd(container.getContainerId()).exec()}
     */
    @Deprecated
    InspectContainerResponse getContainerInfo();

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
     * @return the ports on which to check if the container is ready
     */
    Set<Integer> getLivenessCheckPortNumbers();

    /**
     * Run a command inside a running container, as though using "docker exec", and interpreting
     * the output as UTF8.
     * <p>
     * @see #execInContainer(Charset, String...)
     */
    Container.ExecResult execInContainer(String... command)
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
    Container.ExecResult execInContainer(Charset outputCharset, String... command)
        throws UnsupportedOperationException, IOException, InterruptedException;

    Logger logger();

}
