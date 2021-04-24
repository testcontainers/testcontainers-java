package org.testcontainers.dockerclient;

import com.github.dockerjava.core.LocalDirectorySSLConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.CommandLine;
import org.testcontainers.utility.DockerMachineClient;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Use Docker machine (if available on the PATH) to locate a Docker environment.
 *
 * @deprecated this class is used by the SPI and should not be used directly
 */
@Slf4j
@Deprecated
public final class DockerMachineClientProviderStrategy extends DockerClientProviderStrategy {

    @Getter(lazy = true)
    private final TransportConfig transportConfig = resolveTransportConfig();

    private TransportConfig resolveTransportConfig() throws InvalidConfigurationException {
        boolean installed = DockerMachineClient.instance().isInstalled();
        checkArgument(installed, "docker-machine executable was not found on PATH (" + Arrays.toString(CommandLine.getSystemPath()) + ")");

        Optional<String> machineNameOptional = DockerMachineClient.instance().getDefaultMachine();
        checkArgument(machineNameOptional.isPresent(), "docker-machine is installed but no default machine could be found");
        String machineName = machineNameOptional.get();

        log.info("Found docker-machine, and will use machine named {}", machineName);

        DockerMachineClient.instance().ensureMachineRunning(machineName);

        String dockerDaemonUrl = DockerMachineClient.instance().getDockerDaemonUrl(machineName);

        log.info("Docker daemon URL for docker machine {} is {}", machineName, dockerDaemonUrl);

        return TransportConfig.builder()
            .dockerHost(URI.create(dockerDaemonUrl))
            .sslConfig(
                new LocalDirectorySSLConfig(
                    Paths.get(System.getProperty("user.home") + "/.docker/machine/certs/").toString()
                )
            )
            .build();
    }

    @Override
    protected boolean isApplicable() {
        boolean installed = DockerMachineClient.instance().isInstalled();
        if (!installed) {
            log.info("docker-machine executable was not found on PATH ({})", Arrays.toString(CommandLine.getSystemPath()));
            return false;
        }

        Optional<String> machineNameOptional = DockerMachineClient.instance().getDefaultMachine();
        if (!machineNameOptional.isPresent()) {
            log.info("docker-machine is installed but no default machine could be found");
        }

        return true;
    }

    @Override
    protected boolean isPersistable() {
        return false;
    }

    @Override
    protected int getPriority() {
        return EnvironmentAndSystemPropertyClientProviderStrategy.PRIORITY - 100;
    }

    @Override
    public String getDescription() {
        return "docker-machine";
    }
}
