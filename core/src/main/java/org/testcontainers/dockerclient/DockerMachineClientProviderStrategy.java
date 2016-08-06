package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DockerClientConfig;
import org.testcontainers.utility.CommandLine;
import org.testcontainers.utility.DockerMachineClient;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Use Docker machine (if available on the PATH) to locate a Docker environment.
 */
public class DockerMachineClientProviderStrategy extends DockerClientProviderStrategy {
    @Override
    public void test() throws InvalidConfigurationException {

        try {
            boolean installed = DockerMachineClient.instance().isInstalled();
            checkArgument(installed, "docker-machine executable was not found on PATH (" + Arrays.toString(CommandLine.getSystemPath()) + ")");

            Optional<String> machineNameOptional = DockerMachineClient.instance().getDefaultMachine();
            checkArgument(machineNameOptional.isPresent(), "docker-machine is installed but no default machine could be found");
            String machineName = machineNameOptional.get();

            LOGGER.info("Found docker-machine, and will use machine named {}", machineName);

            DockerMachineClient.instance().ensureMachineRunning(machineName);

            String dockerDaemonIpAddress = DockerMachineClient.instance().getDockerDaemonIpAddress(machineName);

            LOGGER.info("Docker daemon IP address for docker machine {} is {}", machineName, dockerDaemonIpAddress);

            config = DockerClientConfig
                    .createDefaultConfigBuilder()
                    .withDockerHost("tcp://" + dockerDaemonIpAddress + ":2376")
                    .withDockerTlsVerify(true)
                    .withDockerCertPath(Paths.get(System.getProperty("user.home") + "/.docker/machine/certs/").toString())
                    .build();
            client = getClientForConfig(config);
        } catch (Exception e) {
            throw new InvalidConfigurationException(e.getMessage());
        }

        // If the docker-machine VM has started, the docker daemon may still not be ready. Retry pinging until it works.
        ping(client, 30);
    }

    @Override
    public String getDescription() {
        return "docker-machine";
    }
}
