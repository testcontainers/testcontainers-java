package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import org.slf4j.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.utility.TestEnvironment;

import java.io.IOException;
import java.nio.charset.Charset;

public interface CommandExecutor extends ContainerState {

    /**
     * Run a command inside a running container, as though using "docker exec", and interpreting
     * the output as UTF8.
     * <p>
     * @see #execInContainer(Charset, String...)
     */
    default Container.ExecResult execInContainer(String... command)
        throws UnsupportedOperationException, IOException, InterruptedException {
        return execInContainer(Charset.forName("UTF-8"), command);
    }

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
    default Container.ExecResult execInContainer(Charset outputCharset, String... command)
        throws UnsupportedOperationException, IOException, InterruptedException {
        if (!TestEnvironment.dockerExecutionDriverSupportsExec()) {
            // at time of writing, this is the expected result in CircleCI.
            throw new UnsupportedOperationException(
                "Your docker daemon is running the \"lxc\" driver, which doesn't support \"docker exec\".");

        }

        if (!isRunning()) {
            throw new IllegalStateException("execInContainer can only be used while the Container is running");
        }

        DockerClient dockerClient = DockerClientFactory.instance().client();

        dockerClient
            .execCreateCmd(this.getContainerId())
            .withCmd(command);

        Logger logger = getLogger();

        logger.debug("Running \"exec\" command: " + String.join(" ", command));
        final ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(this.getContainerId())
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

        logger.trace("stdout: " + result.getStdout());
        logger.trace("stderr: " + result.getStderr());
        return result;
    }
}
