package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class NamedPipeSocketClientProviderStrategy extends DockerClientProviderStrategy {

    private static final int PING_TIMEOUT_DEFAULT = 5;
    private static final String PING_TIMEOUT_PROPERTY_NAME = "testcontainers.namedpipesocketprovider.timeout";

    @Override
    public void test() throws InvalidConfigurationException {
        if (!new File("\\\\.\\pipe\\docker_engine").exists()) {
            throw new InvalidConfigurationException("this strategy only works with Docker for Windows");
        }

        config = tryConfiguration("tcp://localhost:2375");
    }

    @Override
    public String getDescription() {
        return "Docker for Windows (via TCP port 2375)";
    }

    @NotNull
    protected DockerClientConfig tryConfiguration(String dockerHost) {
        config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withDockerTlsVerify(false)
                .build();
        client = getClientForConfig(config);

        final int timeout = Integer.getInteger(PING_TIMEOUT_PROPERTY_NAME, PING_TIMEOUT_DEFAULT);
        ping(client, timeout);

        return config;
    }
}
