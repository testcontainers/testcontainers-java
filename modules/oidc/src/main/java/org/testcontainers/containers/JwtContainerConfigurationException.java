package org.testcontainers.containers;


import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.Container.ExecResult;


@Getter
@RequiredArgsConstructor
public final class JwtContainerConfigurationException extends RuntimeException {
    private final String action;
    private final int exitCode;
    private final String stdout;
    private final String stderr;

    public JwtContainerConfigurationException(String action, ExecResult result) {
        this(action, result.getExitCode(), result.getStdout(), result.getStderr());
    }

    @Override
    public String getMessage() {
        return "Container Configuration failed on action: " + action + "\n" +
            "Exit code was: " + exitCode + "\n" +
            "Stdout: " + stdout + "\n" +
            "Stderr: " + stderr + "\n" +
            "Parent Message:" + super.getMessage();
    }
}
