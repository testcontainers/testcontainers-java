package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.netty.DockerCmdExecFactoryImpl;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UnixSocketConfigurationStrategy implements DockerConfigurationStrategy {

    public static final String DOCKER_SOCK_PATH = "/var/run/docker.sock";
    public static final String SOCKET_LOCATION = "unix://" + DOCKER_SOCK_PATH;

    @Override
    public DockerClientConfig provideConfiguration()
            throws InvalidConfigurationException {

        if (!System.getProperty("os.name").toLowerCase().contains("linux")) {
            throw new InvalidConfigurationException("this strategy is only applicable to Linux");
        }

        DockerClientConfig config;
        try {
            config = tryConfiguration(new DockerCmdExecFactoryImpl(), SOCKET_LOCATION);
            LOGGER.info("Accessing docker with local Unix socket");
            return config;
        } catch (Exception e) {
            throw new InvalidConfigurationException("ping failed", e);
        }
    }

    @NotNull
    protected DockerClientConfig tryConfiguration(DockerCmdExecFactory cmdExecFactory, String dockerHost) {

        Path dockerSocketFile = Paths.get(DOCKER_SOCK_PATH);
        Integer mode;
        try {
            mode = (Integer) Files.getAttribute(dockerSocketFile, "unix:mode");
        } catch (IOException e) {
            throw new InvalidConfigurationException("Could not find unix domain socket", e);
        }

        if (mode != 0140755) { // simple comparison; could be improved with bitmasking
            throw new InvalidConfigurationException("Found docker unix domain socket but file mode was not as expected (expected: srwxr-xr-x). This problem is possibly due to occurrence of this issue in the past: https://github.com/docker/docker/issues/13121");
        }

        DockerClientConfig config;
        config = new DockerClientConfig.DockerClientConfigBuilder()
                .withDockerHost(dockerHost)
                .withDockerTlsVerify(false)
                .build();
        DockerClient client = DockerClientBuilder
                .getInstance(config)
                .withDockerCmdExecFactory(cmdExecFactory)
                .build();

        client.pingCmd().exec();
        return config;
    }

    @Override
    public String getDescription() {
        return "local Unix socket (" + SOCKET_LOCATION + ")";
    }

}
