package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.rnorth.tcpunixsocketproxy.TcpToUnixSocketProxy;

import java.io.IOException;

public class UnixSocketConfigurationStrategy implements DockerConfigurationStrategy {

    public static final String SOCKET_LOCATION = "unix:///var/run/docker.sock";

    @Override
    public DockerClientConfig provideConfiguration()
            throws InvalidConfigurationException {

        DockerClientConfig config;
        TcpToUnixSocketProxy proxy;
        try {
            proxy = new TcpToUnixSocketProxy("localhost", 0, "/var/run/docker.sock");
        } catch (IOException e) {
            throw new InvalidConfigurationException("TCP-UNIX socket proxy creation failed", e);
        }

        try {
            int port = proxy.start();

            config = new DockerClientConfig.DockerClientConfigBuilder().withDockerHost("tcp://localhost:" + port).build();
            DockerClient client = DockerClientBuilder.getInstance(config).build();

            client.pingCmd().exec();

        } catch (Exception e) {

            proxy.stop();

            throw new InvalidConfigurationException("ping failed", e);
        }

        LOGGER.info("Accessing docker with local Unix socket");
        return config;
    }

    @Override
    public String getDescription() {
        return "local Unix socket (" + SOCKET_LOCATION + ")";
    }

}
