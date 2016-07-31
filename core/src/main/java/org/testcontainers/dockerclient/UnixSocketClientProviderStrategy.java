package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DockerClientConfig;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UnixSocketClientProviderStrategy extends DockerClientProviderStrategy {

    protected static final String DOCKER_SOCK_PATH = "/var/run/docker.sock";
    private static final String SOCKET_LOCATION = "unix://" + DOCKER_SOCK_PATH;
    private static final int SOCKET_FILE_MODE_MASK = 0xc000;

    @Override
    public void test()
            throws InvalidConfigurationException {

        if (!System.getProperty("os.name").toLowerCase().contains("linux")) {
            throw new InvalidConfigurationException("this strategy is only applicable to Linux");
        }

        try {
            config = tryConfiguration(SOCKET_LOCATION);
            LOGGER.info("Accessing docker with local Unix socket");
        } catch (Exception e) {
            throw new InvalidConfigurationException("ping failed", e);
        }
    }

    @NotNull
    protected DockerClientConfig tryConfiguration(String dockerHost) {

        Path dockerSocketFile = Paths.get(DOCKER_SOCK_PATH);
        Integer mode;
        try {
            mode = (Integer) Files.getAttribute(dockerSocketFile, "unix:mode");
        } catch (IOException e) {
            throw new InvalidConfigurationException("Could not find unix domain socket", e);
        }

        if ((mode & 0xc000) != SOCKET_FILE_MODE_MASK) {
            throw new InvalidConfigurationException("Found docker unix domain socket but file mode was not as expected (expected: srwxr-xr-x). This problem is possibly due to occurrence of this issue in the past: https://github.com/docker/docker/issues/13121");
        }

        config = DockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withDockerTlsVerify(false)
                .build();
        client = getClientForConfig(config);

        ping(client, 3);

        return config;
    }

    @Override
    public String getDescription() {
        return "local Unix socket (" + SOCKET_LOCATION + ")";
    }

}
