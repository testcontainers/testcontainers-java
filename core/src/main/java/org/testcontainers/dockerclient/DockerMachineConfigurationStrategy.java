package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.utility.CommandLine;
import org.testcontainers.utility.DockerMachineClient;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Use Docker machine (if available on the PATH) to locate a Docker environment.
 */
public class DockerMachineConfigurationStrategy implements DockerConfigurationStrategy {
    @Override
    public DockerClientConfig provideConfiguration() throws InvalidConfigurationException {

        DockerClientConfig candidateConfig;
        DockerClient client;

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

            candidateConfig = DockerClientConfig
                    .createDefaultConfigBuilder()
                    .withUri("https://" + dockerDaemonIpAddress + ":2376")
                    .withDockerCertPath(Paths.get(System.getProperty("user.home") + "/.docker/machine/certs/").toString())
                    .build();
            client = DockerClientBuilder.getInstance(candidateConfig).build();
        } catch (Exception e) {
            throw new InvalidConfigurationException(e.getMessage());
        }

        // If the docker-machine VM has started, the docker daemon may still not be ready. Retry pinging until it works.
        Unreliables.retryUntilSuccess(30, TimeUnit.SECONDS, () -> {
            client.pingCmd().exec();
            return true;
        });

        return candidateConfig;
    }

    @Override
    public String getDescription() {
        return "docker-machine";
    }
}
