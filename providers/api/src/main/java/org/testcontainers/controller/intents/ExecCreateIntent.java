package org.testcontainers.controller.intents;


import com.github.dockerjava.api.command.AsyncDockerCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;

public interface ExecCreateIntent {
    ExecCreateIntent withAttachStdout(boolean attachStdout);

    ExecCreateIntent withAttachStderr(boolean attachStderr);

    ExecCreateIntent withCmd(String... command);

    ExecCreateResult exec(); // TODO: Rename
}
