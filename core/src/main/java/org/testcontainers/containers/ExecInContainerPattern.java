package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmd;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides utility methods for executing commands in containers
 */
@UtilityClass
@Slf4j
public class ExecInContainerPattern {

    /**
     *
     * @deprecated use {@link #execInContainer(DockerClient, InspectContainerResponse, String...)}
     */
    @Deprecated
    public Container.ExecResult execInContainer(InspectContainerResponse containerInfo, String... command)
        throws UnsupportedOperationException, IOException, InterruptedException {
        DockerClient dockerClient = DockerClientFactory.instance().client();
        return execInContainer(dockerClient, containerInfo, command);
    }

    /**
     *
     * @deprecated use {@link #execInContainer(DockerClient, InspectContainerResponse, Charset, String...)}
     */
    @Deprecated
    public Container.ExecResult execInContainer(
        InspectContainerResponse containerInfo,
        Charset outputCharset,
        String... command
    ) throws UnsupportedOperationException, IOException, InterruptedException {
        DockerClient dockerClient = DockerClientFactory.instance().client();
        return execInContainerWithUser(dockerClient, containerInfo, outputCharset, null, command);
    }

    /**
     * Run a command inside a running container, as though using "docker exec", and interpreting
     * the output as UTF8.
     * <p></p>
     * @param dockerClient the {@link DockerClient}
     * @param containerInfo the container info
     * @param command the command to execute
     * @see #execInContainerWithUser(DockerClient, InspectContainerResponse, String, String...)
     */
    public Container.ExecResult execInContainer(
        DockerClient dockerClient,
        InspectContainerResponse containerInfo,
        String... command
    ) throws UnsupportedOperationException, IOException, InterruptedException {
        return execInContainerWithUser(dockerClient, containerInfo, StandardCharsets.UTF_8, null, command);
    }

    /**
     * Run a command inside a running container, as though using "docker exec", and interpreting
     * the output as UTF8.
     * <p></p>
     * @param dockerClient the {@link DockerClient}
     * @param containerInfo the container info
     * @param outputCharset the character set used to interpret the output.
     * @param command the command to execute
     * @see #execInContainerWithUser(DockerClient, InspectContainerResponse, Charset, String, String...)
     */
    public Container.ExecResult execInContainer(
        DockerClient dockerClient,
        InspectContainerResponse containerInfo,
        Charset outputCharset,
        String... command
    ) throws UnsupportedOperationException, IOException, InterruptedException {
        return execInContainerWithUser(dockerClient, containerInfo, outputCharset, null, command);
    }

    /**
     * Run a command inside a running container as a given user, as using "docker exec -u user" and
     * interpreting the output as UTF8.
     * <p>
     * This functionality is not available on a docker daemon running the older "lxc" execution driver. At
     * the time of writing, CircleCI was using this driver.
     * @param dockerClient the {@link DockerClient}
     * @param containerInfo the container info
     * @param user the user to run the command with, optional
     * @param command the command to execute
     * @see #execInContainerWithUser(DockerClient, InspectContainerResponse, Charset, String,
     *     String...)
     * @deprecated use {@link #execInContainer(DockerClient, InspectContainerResponse, ExecConfig)}
     */
    @Deprecated
    public Container.ExecResult execInContainerWithUser(
        DockerClient dockerClient,
        InspectContainerResponse containerInfo,
        String user,
        String... command
    ) throws UnsupportedOperationException, IOException, InterruptedException {
        return execInContainerWithUser(dockerClient, containerInfo, StandardCharsets.UTF_8, user, command);
    }

    /**
     * Run a command inside a running container as a given user, as using "docker exec -u user".
     * <p>
     * This functionality is not available on a docker daemon running the older "lxc" execution
     * driver. At the time of writing, CircleCI was using this driver.
     * @param dockerClient the {@link DockerClient}
     * @param containerInfo the container info
     * @param outputCharset the character set used to interpret the output.
     * @param user the user to run the command with, optional
     * @param command the parts of the command to run
     * @return the result of execution
     * @throws IOException if there's an issue communicating with Docker
     * @throws InterruptedException if the thread waiting for the response is interrupted
     * @throws UnsupportedOperationException if the docker daemon you're connecting to doesn't support "exec".
     * @deprecated use {@link #execInContainer(DockerClient, InspectContainerResponse, Charset, ExecConfig)}
     */
    @Deprecated
    public Container.ExecResult execInContainerWithUser(
        DockerClient dockerClient,
        InspectContainerResponse containerInfo,
        Charset outputCharset,
        String user,
        String... command
    ) throws UnsupportedOperationException, IOException, InterruptedException {
        return execInContainer(
            dockerClient,
            containerInfo,
            outputCharset,
            ExecConfig.builder().user(user).command(command).build()
        );
    }

    /**
     * Run a command inside a running container as a given user, as using "docker exec -u user".
     * <p>
     * This functionality is not available on a docker daemon running the older "lxc" execution
     * driver. At the time of writing, CircleCI was using this driver.
     * @param dockerClient the {@link DockerClient}
     * @param containerInfo the container info
     * @param execConfig the exec configuration
     * @return the result of execution
     * @throws IOException if there's an issue communicating with Docker
     * @throws InterruptedException if the thread waiting for the response is interrupted
     * @throws UnsupportedOperationException if the docker daemon you're connecting to doesn't support "exec".
     */
    public Container.ExecResult execInContainer(
        DockerClient dockerClient,
        InspectContainerResponse containerInfo,
        ExecConfig execConfig
    ) throws UnsupportedOperationException, IOException, InterruptedException {
        return execInContainer(dockerClient, containerInfo, StandardCharsets.UTF_8, execConfig);
    }

    /**
     * Run a command inside a running container as a given user, as using "docker exec -u user".
     * <p>
     * This functionality is not available on a docker daemon running the older "lxc" execution
     * driver. At the time of writing, CircleCI was using this driver.
     * @param dockerClient the {@link DockerClient}
     * @param containerInfo the container info
     * @param outputCharset the character set used to interpret the output.
     * @param execConfig the exec configuration
     * @return the result of execution
     * @throws IOException if there's an issue communicating with Docker
     * @throws InterruptedException if the thread waiting for the response is interrupted
     * @throws UnsupportedOperationException if the docker daemon you're connecting to doesn't support "exec".
     */
    public Container.ExecResult execInContainer(
        DockerClient dockerClient,
        InspectContainerResponse containerInfo,
        Charset outputCharset,
        ExecConfig execConfig
    ) throws UnsupportedOperationException, IOException, InterruptedException {
        if (!TestEnvironment.dockerExecutionDriverSupportsExec()) {
            // at time of writing, this is the expected result in CircleCI.
            throw new UnsupportedOperationException(
                "Your docker daemon is running the \"lxc\" driver, which doesn't support \"docker exec\"."
            );
        }

        if (!isRunning(containerInfo)) {
            throw new IllegalStateException("execInContainer can only be used while the Container is running");
        }

        String containerId = containerInfo.getId();
        String containerName = containerInfo.getName();

        String[] command = execConfig.getCommand();
        log.debug("{}: Running \"exec\" command: {}", containerName, String.join(" ", command));
        final ExecCreateCmd execCreateCmd = dockerClient
            .execCreateCmd(containerId)
            .withAttachStdout(true)
            .withAttachStderr(true)
            .withCmd(command);

        String user = execConfig.getUser();
        if (user != null && !user.isEmpty()) {
            log.debug("{}: Running \"exec\" command with user: {}", containerName, user);
            execCreateCmd.withUser(user);
        }

        String workDir = execConfig.getWorkDir();
        if (workDir != null && !workDir.isEmpty()) {
            log.debug("{}: Running \"exec\" command inside workingDir: {}", containerName, workDir);
            execCreateCmd.withWorkingDir(workDir);
        }

        Map<String, String> envVars = execConfig.getEnvVars();
        if (envVars != null && !envVars.isEmpty()) {
            List<String> envVarList = envVars
                .entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.toList());
            execCreateCmd.withEnv(envVarList);
        }

        final ExecCreateCmdResponse execCreateCmdResponse = execCreateCmd.exec();

        final ToStringConsumer stdoutConsumer = new ToStringConsumer();
        final ToStringConsumer stderrConsumer = new ToStringConsumer();

        try (FrameConsumerResultCallback callback = new FrameConsumerResultCallback()) {
            callback.addConsumer(OutputFrame.OutputType.STDOUT, stdoutConsumer);
            callback.addConsumer(OutputFrame.OutputType.STDERR, stderrConsumer);

            dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback).awaitCompletion();
        }
        int exitCode = dockerClient.inspectExecCmd(execCreateCmdResponse.getId()).exec().getExitCodeLong().intValue();

        final Container.ExecResult result = new Container.ExecResult(
            exitCode,
            stdoutConsumer.toString(outputCharset),
            stderrConsumer.toString(outputCharset)
        );

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
