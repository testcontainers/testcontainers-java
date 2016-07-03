package org.testcontainers.dockerclient;

import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DockerClientConfig;
import org.rnorth.tcpunixsocketproxy.TcpToUnixSocketProxy;

import java.io.File;

public class ProxiedUnixSocketConfigurationStrategy extends UnixSocketConfigurationStrategy
        implements DockerConfigurationStrategy {

    @Override
    public DockerClientConfig provideConfiguration(DockerCmdExecFactory cmdExecFactory)
            throws InvalidConfigurationException {

        if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
            throw new InvalidConfigurationException("this strategy is only applicable to OS X");
        }

        TcpToUnixSocketProxy proxy;
        proxy = new TcpToUnixSocketProxy(new File(DOCKER_SOCK_PATH));

        int proxyPort;
        try {
            proxyPort = proxy.start().getPort();
            DockerClientConfig config = tryConfiguration(cmdExecFactory, "tcp://localhost:" + proxyPort);

            LOGGER.info("Accessing Docker for Mac unix domain socket via TCP proxy (" + DOCKER_SOCK_PATH + " via localhost:" + proxyPort + ")");
            return config;
        } catch (Exception e) {

            proxy.stop();

            throw new InvalidConfigurationException("ping failed", e);
        }

    }

    @Override
    public String getDescription() {
        return "local Unix socket (via TCP proxy)";
    }

}
