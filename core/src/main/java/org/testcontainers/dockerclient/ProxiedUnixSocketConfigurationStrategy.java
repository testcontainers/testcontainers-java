package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.rnorth.tcpunixsocketproxy.TcpToUnixSocketProxy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProxiedUnixSocketConfigurationStrategy implements DockerConfigurationStrategy {

    public static final String SOCKET_LOCATION = "/var/run/docker.sock";
    private int proxyPort;

    @Override
    public DockerClientConfig provideConfiguration()
            throws InvalidConfigurationException {

        if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
            throw new InvalidConfigurationException("this strategy is only applicable to OS X");
        }

        Path dockerSocketFile = Paths.get("/var/run/docker.sock");
        Integer mode;
        try {
            mode = (Integer) Files.getAttribute(dockerSocketFile, "unix:mode");
        } catch (IOException e) {
            throw new InvalidConfigurationException("Could not find unix domain socket", e);
        }

        if (mode != 0140755) { // simple comparison; could be improved with bitmasking
            throw new InvalidConfigurationException("Found docker unix domain socket but file mode was not as expected (expected: srwxr-xr-x)");
        }

        DockerClientConfig config;
        TcpToUnixSocketProxy proxy;
        try {
            proxy = new TcpToUnixSocketProxy("localhost", 0, SOCKET_LOCATION);
        } catch (IOException e) {
            throw new InvalidConfigurationException("TCP-UNIX socket proxy creation failed", e);
        }

        try {
            proxyPort = proxy.start();

            config = new DockerClientConfig.DockerClientConfigBuilder().withDockerHost("tcp://localhost:" + proxyPort).build();
            DockerClient client = DockerClientBuilder.getInstance(config).build();

            client.pingCmd().exec();

        } catch (Exception e) {

            proxy.stop();

            throw new InvalidConfigurationException("ping failed", e);
        }

        LOGGER.info("Accessing Docker for Mac unix domain socket via TCP proxy (" + SOCKET_LOCATION + " via localhost:" + proxyPort + ")");
        return config;
    }

    @Override
    public String getDescription() {
        return "local Unix socket via TCP proxy)";
    }

}
