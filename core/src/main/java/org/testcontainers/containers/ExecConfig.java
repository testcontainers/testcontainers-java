package org.testcontainers.containers;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Exec configuration.
 */
@Builder
@Getter
public class ExecConfig {

    /**
     * The command to run.
     */
    private String[] command;

    /**
     * The user to run the exec process.
     */
    private String user;

    /**
     * Key-value pairs of environment variables.
     */
    private Map<String, String> envVars;

    /**
     * The working directory for the exec process.
     */
    private String workDir;
}
