package org.testcontainers.utility;

import lombok.NonNull;
import org.slf4j.Logger;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;
import static org.testcontainers.utility.CommandLine.executableExists;
import static org.testcontainers.utility.CommandLine.runShellCommand;

/**
 * Created by rnorth on 27/10/2015.
 */
public class DockerMachineClient {

    private static DockerMachineClient instance;
    private static final Logger LOGGER = getLogger(DockerMachineClient.class);

    /**
     * Private constructor
     */
    private DockerMachineClient() {
    }

    /**
     * Obtain an instance of the DockerMachineClient wrapper.
     *
     * @return the singleton instance of DockerMachineClient
     */
    public synchronized static DockerMachineClient instance() {
        if (instance == null) {
            instance = new DockerMachineClient();
        }

        return instance;
    }

    public boolean isInstalled() {
        return executableExists("docker-machine");
    }

    public Optional<String> getDefaultMachine() {
        String ls = runShellCommand("docker-machine", "ls", "-q");
        String[] machineNames = ls.split("\n");

        if (machineNames.length > 0) {
            return Optional.of(machineNames[0]);
        } else {
            return Optional.empty();
        }
    }

    public void ensureMachineRunning(@NonNull String machineName) {
        String status = runShellCommand("docker-machine", "status", machineName);
        if (!status.trim().equalsIgnoreCase("running")) {
            LOGGER.info("Docker-machine '{}' is not running. Current status is '{}'. Will start it now", machineName, status);
            runShellCommand("docker-machine", "start", machineName);
        }
    }

    public String getDockerDaemonIpAddress(@NonNull String machineName) {
        return runShellCommand("docker-machine", "ip", machineName);
    }
}
