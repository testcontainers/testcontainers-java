package org.testcontainers.utility;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
class RyukClient {

    private final DockerClient dockerClient;

    private final String ryukContainerId;

    public RyukClient(DockerClient dockerClient, String ryukContainerId) {
        this.dockerClient = dockerClient;
        this.ryukContainerId = ryukContainerId;
    }

    public <T extends ResultCallback<Frame>> T acknowledge(String query, T callback) {
        ExecCreateCmdResponse exec = dockerClient.execCreateCmd(ryukContainerId)
            .withAttachStdin(true)
            .withAttachStdout(true)
            .withCmd("nc", "localhost", "8080")
            .exec();

        return dockerClient.execStartCmd(exec.getId())
            .withStdIn(new ByteArrayInputStream((query + "\n").getBytes(StandardCharsets.UTF_8)))
            .exec(callback);
    }
}
