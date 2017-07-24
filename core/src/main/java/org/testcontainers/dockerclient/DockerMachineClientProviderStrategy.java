package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.CommandLine;
import org.testcontainers.utility.DockerMachineClient;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Use Docker machine (if available on the PATH) to locate a Docker environment.
 */
@Slf4j
public class DockerMachineClientProviderStrategy extends DockerClientProviderStrategy {
    private static final String PING_TIMEOUT_DEFAULT = "30";
    private static final String PING_TIMEOUT_PROPERTY_NAME = "testcontainers.dockermachineprovider.timeout";

    @Override
    protected boolean isApplicable() {
        return DockerMachineClient.instance().isInstalled();
    }

    @Override
    protected int getPriority() {
        return ProxiedUnixSocketClientProviderStrategy.PRIORITY - 10;
    }

    @Override
    public void test() throws InvalidConfigurationException {

        try {
            boolean installed = DockerMachineClient.instance().isInstalled();
            checkArgument(installed, "docker-machine executable was not found on PATH (" + Arrays.toString(CommandLine.getSystemPath()) + ")");

            Optional<String> machineNameOptional = DockerMachineClient.instance().getDefaultMachine();
            checkArgument(machineNameOptional.isPresent(), "docker-machine is installed but no default machine could be found");
            String machineName = machineNameOptional.get();

            log.info("Found docker-machine, and will use machine named {}", machineName);

            DockerMachineClient.instance().ensureMachineRunning(machineName);

            String dockerDaemonIpAddress = DockerMachineClient.instance().getDockerDaemonIpAddress(machineName);

            log.info("Docker daemon IP address for docker machine {} is {}", machineName, dockerDaemonIpAddress);

            config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost("tcp://" + dockerDaemonIpAddress + ":2376")
                    .withDockerTlsVerify(true)
                    .withDockerCertPath(Paths.get(System.getProperty("user.home") + "/.docker/machine/certs/").toString())
                    .build();
            client = getClientForConfig(config);
        } catch (Exception e) {
            throw new InvalidConfigurationException(e.getMessage());
        }

        // If the docker-machine VM has started, the docker daemon may still not be ready. Retry pinging until it works.
        final int timeout = Integer.parseInt(System.getProperty(PING_TIMEOUT_PROPERTY_NAME, PING_TIMEOUT_DEFAULT));
        ping(client, timeout);
    }

    @Override
    public String getDescription() {
        return "docker-machine";
    }
}
