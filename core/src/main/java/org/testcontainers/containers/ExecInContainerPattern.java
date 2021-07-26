package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.utility.TestEnvironment;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Provides utility methods for executing commands in containers
 */
@UtilityClass
@Slf4j
public class ExecInContainerPattern {

    /**
     * Run a command inside a running container, as though using "docker exec", and interpreting
     * the output as UTF8.
     * <p></p>
     * @param containerInfo the container info
     * @param command the command to execute
     * @see #execInContainer(InspectContainerResponse, Charset, String...)
     */
    public Container.ExecResult execInContainer(InspectContainerResponse containerInfo, String... command)
        throws UnsupportedOperationException, IOException, InterruptedException {
        return execInContainer(containerInfo, Charset.forName("UTF-8"), command);
    }

    /**
     * Run a command inside a running container, as though using "docker exec".
     * <p>
     * This functionality is not available on a docker daemon running the older "lxc" execution driver. At
     * the time of writing, CircleCI was using this driver.
     * @param containerInfo the container info
     * @param outputCharset the character set used to interpret the output.
     * @param command the parts of the command to run
     * @return the result of execution
     * @throws IOException if there's an issue communicating with Docker
     * @throws InterruptedException if the thread waiting for the response is interrupted
     * @throws UnsupportedOperationException if the docker daemon you're connecting to doesn't support "exec".
     */
    public Container.ExecResult execInContainer(InspectContainerResponse containerInfo, Charset outputCharset, String... command)
        throws UnsupportedOperationException, IOException, InterruptedException {
        if (!TestEnvironment.dockerExecutionDriverSupportsExec()) {
            // at time of writing, this is the expected result in CircleCI.
            throw new UnsupportedOperationException(
                "Your docker daemon is running the \"lxc\" driver, which doesn't support \"docker exec\".");

        }

        if (!isRunning(containerInfo)) {
            throw new IllegalStateException("execInContainer can only be used while the Container is running");
        }

        String containerId = containerInfo.getId();
        String containerName = containerInfo.getName();

        DockerClient dockerClient = DockerClientFactory.instance().client();

        log.debug("{}: Running \"exec\" command: {}", containerName, String.join(" ", command));
        final ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
            .withAttachStdout(true).withAttachStderr(true).withCmd(command).exec();

        final ToStringConsumer stdoutConsumer = new ToStringConsumer();
        final ToStringConsumer stderrConsumer = new ToStringConsumer();

        try (FrameConsumerResultCallback callback = new FrameConsumerResultCallback()) {
            callback.addConsumer(OutputFrame.OutputType.STDOUT, stdoutConsumer);
            callback.addConsumer(OutputFrame.OutputType.STDERR, stderrConsumer);

            dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback).awaitCompletion();
        }
        Integer exitCode = dockerClient.inspectExecCmd(execCreateCmdResponse.getId()).exec().getExitCode();

        final Container.ExecResult result = new Container.ExecResult(
            exitCode,
            stdoutConsumer.toString(outputCharset),
            stderrConsumer.toString(outputCharset));

        log.trace("{}: stdout: {}", containerName, result.getStdout());
        log.trace("{}: stderr: {}", containerName, result.getStderr());
        return result;
    }

    private boolean isRunning(InspectContainerResponse containerInfo) {
        try {
            return containerInfo != null && containerInfo.getState().getRunning();
        } catch (DockerException e) {
            return false;
        }
    }
}
