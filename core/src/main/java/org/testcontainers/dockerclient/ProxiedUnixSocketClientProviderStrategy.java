package org.testcontainers.dockerclient;

import org.rnorth.tcpunixsocketproxy.TcpToUnixSocketProxy;

import java.io.File;

public class ProxiedUnixSocketClientProviderStrategy extends UnixSocketClientProviderStrategy {

    @Override
    public void test() throws InvalidConfigurationException {

        if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
            throw new InvalidConfigurationException("this strategy is only applicable to OS X");
        }

        TcpToUnixSocketProxy proxy = new TcpToUnixSocketProxy(new File(DOCKER_SOCK_PATH));

        try {
            int proxyPort = proxy.start().getPort();

            config = tryConfiguration("tcp://localhost:" + proxyPort);

            LOGGER.info("Accessing Docker for Mac unix domain socket via TCP proxy (" + DOCKER_SOCK_PATH + " via localhost:" + proxyPort + ")");
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
