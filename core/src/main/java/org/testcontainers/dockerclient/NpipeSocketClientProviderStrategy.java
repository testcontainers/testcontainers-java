package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.net.URI;

@Slf4j
public class NpipeSocketClientProviderStrategy extends DockerClientProviderStrategy {

    protected static final String DOCKER_SOCK_PATH = "//./pipe/docker_engine";
    private static final String SOCKET_LOCATION = "npipe://" + DOCKER_SOCK_PATH;

    private static final String PING_TIMEOUT_DEFAULT = "10";
    private static final String PING_TIMEOUT_PROPERTY_NAME = "testcontainers.npipesocketprovider.timeout";

    public static final int PRIORITY = EnvironmentAndSystemPropertyClientProviderStrategy.PRIORITY - 20;

    @Override
    protected boolean isApplicable() {
        return SystemUtils.IS_OS_WINDOWS && TestcontainersConfiguration.getInstance().isNpipeForced();
    }

    @Override
    public void test() throws InvalidConfigurationException {
        try {
            config = tryConfiguration();
            log.info("Accessing docker with {}", getDescription());
        } catch (Exception | UnsatisfiedLinkError e) {
            throw new InvalidConfigurationException("ping failed", e);
        }
    }

    @NotNull
    private DockerClientConfig tryConfiguration() {
        URI dockerHost = URI.create(SOCKET_LOCATION);

        config = new DelegatingDockerClientConfig(
            DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://localhost:0")
                .withDockerTlsVerify(false)
                .build()
        ) {
            @Override
            public URI getDockerHost() {
                return dockerHost;
            }
        };
        client = getClientForConfig(config);

        final int timeout = Integer.parseInt(System.getProperty(PING_TIMEOUT_PROPERTY_NAME, PING_TIMEOUT_DEFAULT));
        ping(client, timeout);

        return config;
    }

    @Override
    public String getDescription() {
        return "local Npipe socket (" + SOCKET_LOCATION + ")";
    }

    @Override
    protected int getPriority() {
        return PRIORITY;
    }

    @RequiredArgsConstructor
    private static class DelegatingDockerClientConfig implements DockerClientConfig {

        @Delegate
        final DockerClientConfig dockerClientConfig;
    }
}
