package org.testcontainers.containers;

import com.github.dockerjava.core.LocalDirectorySSLConfig;
import com.github.dockerjava.transport.SSLConfig;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.dockerclient.TransportConfig;
import org.testcontainers.utility.CommandLine;
import org.testcontainers.utility.DockerLoggerFactory;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Use local Docker Compose binary, if present.
 */
class LocalDockerCompose implements DockerCompose {

    private final List<File> composeFiles;

    private final String identifier;

    private String cmd = "";

    private Map<String, String> env = new HashMap<>();

    private final String composeExecutable;

    public LocalDockerCompose(String composeExecutable, List<File> composeFiles, String identifier) {
        this.composeExecutable = composeExecutable;
        this.composeFiles = composeFiles;
        this.identifier = identifier;
    }

    @Override
    public DockerCompose withCommand(String cmd) {
        this.cmd = cmd;
        return this;
    }

    @Override
    public DockerCompose withEnv(Map<String, String> env) {
        this.env = env;
        return this;
    }

    @Override
    public void invoke() {
        // bail out early
        if (!CommandLine.executableExists(this.composeExecutable)) {
            throw new ContainerLaunchException(
                "Local Docker Compose not found. Is " + this.composeExecutable + " on the PATH?"
            );
        }

        final Map<String, String> environment = Maps.newHashMap(env);
        environment.put(ENV_PROJECT_NAME, identifier);

        TransportConfig transportConfig = DockerClientFactory.instance().getTransportConfig();
        SSLConfig sslConfig = transportConfig.getSslConfig();
        if (sslConfig != null) {
            if (sslConfig instanceof LocalDirectorySSLConfig) {
                environment.put("DOCKER_CERT_PATH", ((LocalDirectorySSLConfig) sslConfig).getDockerCertPath());
                environment.put("DOCKER_TLS_VERIFY", "true");
            } else {
                logger()
                    .warn(
                        "Couldn't set DOCKER_CERT_PATH. `sslConfig` is present but it's not LocalDirectorySSLConfig."
                    );
            }
        }
        String dockerHost = transportConfig.getDockerHost().toString();
        environment.put("DOCKER_HOST", dockerHost);

        final Stream<String> absoluteDockerComposeFilePaths = composeFiles
            .stream()
            .map(File::getAbsolutePath)
            .map(Objects::toString);

        final String composeFileEnvVariableValue = absoluteDockerComposeFilePaths.collect(
            Collectors.joining(File.pathSeparator + "")
        );
        logger().debug("Set env COMPOSE_FILE={}", composeFileEnvVariableValue);

        final File pwd = composeFiles.get(0).getAbsoluteFile().getParentFile().getAbsoluteFile();
        environment.put(ENV_COMPOSE_FILE, composeFileEnvVariableValue);

        logger().info("Local Docker Compose is running command: {}", cmd);

        final List<String> command = Splitter
            .onPattern(" ")
            .omitEmptyStrings()
            .splitToList(this.composeExecutable + " " + cmd);

        try {
            new ProcessExecutor()
                .command(command)
                .redirectOutput(Slf4jStream.of(logger()).asInfo())
                .redirectError(Slf4jStream.of(logger()).asInfo()) // docker-compose will log pull information to stderr
                .environment(environment)
                .directory(pwd)
                .exitValueNormal()
                .executeNoTimeout();

            logger().info("Docker Compose has finished running");
        } catch (InvalidExitValueException e) {
            throw new ContainerLaunchException(
                "Local Docker Compose exited abnormally with code " +
                e.getExitValue() +
                " whilst running command: " +
                cmd
            );
        } catch (Exception e) {
            throw new ContainerLaunchException("Error running local Docker Compose command: " + cmd, e);
        }
    }

    /**
     * @return a logger
     */
    private Logger logger() {
        return DockerLoggerFactory.getLogger(this.composeExecutable);
    }
}
