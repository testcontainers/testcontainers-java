package org.testcontainers.dockerclient;

import lombok.extern.slf4j.Slf4j;
import org.rnorth.tcpunixsocketproxy.TcpToUnixSocketProxy;

import java.io.File;

@Slf4j
public class ProxiedUnixSocketClientProviderStrategy extends UnixSocketClientProviderStrategy {

    public static final int PRIORITY = EnvironmentAndSystemPropertyClientProviderStrategy.PRIORITY - 100;

    private final File socketFile = new File(DOCKER_SOCK_PATH);

    @Override
    protected boolean isApplicable() {
        return socketFile.exists();
    }

    @Override
    protected int getPriority() {
        return PRIORITY;
    }

    @Override
    public void test() throws InvalidConfigurationException {
        TcpToUnixSocketProxy proxy = new TcpToUnixSocketProxy(socketFile);

        try {
            int proxyPort = proxy.start().getPort();

            config = tryConfiguration("tcp://localhost:" + proxyPort);

            log.debug("Accessing unix domain socket via TCP proxy (" + DOCKER_SOCK_PATH + " via localhost:" + proxyPort + ")");
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
