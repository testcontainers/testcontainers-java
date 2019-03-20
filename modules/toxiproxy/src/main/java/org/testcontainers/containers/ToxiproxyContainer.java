package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import eu.rekawek.toxiproxy.model.ToxicList;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Container for resiliency testing using <a href="https://github.com/Shopify/toxiproxy">Toxiproxy</a>.
 */
public class ToxiproxyContainer extends GenericContainer<ToxiproxyContainer> {

    private static final String IMAGE_NAME = "shopify/toxiproxy:2.1.0";
    private static final int TOXIPROXY_CONTROL_PORT = 8474;
    private static final int FIRST_PROXIED_PORT = 8666;
    private static final int LAST_PROXIED_PORT = 8666 + 31;

    private ToxiproxyClient client;
    private final Map<String, ContainerProxy> proxies = new HashMap<>();
    private final AtomicInteger nextPort = new AtomicInteger(FIRST_PROXIED_PORT);

    public ToxiproxyContainer() {
        super(IMAGE_NAME);
    }

    public ToxiproxyContainer(String imageName) {
        super(imageName);
    }

    @Override
    protected void configure() {
        addExposedPorts(TOXIPROXY_CONTROL_PORT);
        setWaitStrategy(new HttpWaitStrategy().forPath("/version").forPort(TOXIPROXY_CONTROL_PORT));

        // allow up to 32 ports to be proxied (arbitrary value). Here we make the ports exposed; whether or not
        //  Toxiproxy will listen is controlled at runtime using getProxy(...)
        for (int i = FIRST_PROXIED_PORT; i <= LAST_PROXIED_PORT; i++) {
            addExposedPort(i);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        client = new ToxiproxyClient(getContainerIpAddress(), getMappedPort(TOXIPROXY_CONTROL_PORT));
    }

    public ContainerProxy getProxy(GenericContainer container, int port) {
        String upstream = container.getNetworkAliases().get(0) + ":" + port;

        return proxies.computeIfAbsent(upstream, __ -> {
            try {
                final int toxiPort = nextPort.getAndIncrement();
                if (toxiPort > LAST_PROXIED_PORT) {
                    throw new IllegalStateException("Maximum number of proxies exceeded");
                }

                final Proxy proxy = client.createProxy("name", "0.0.0.0:" + toxiPort, upstream);
                return new ContainerProxy(proxy, getContainerIpAddress(), getMappedPort(toxiPort));
            } catch (IOException e) {
                throw new RuntimeException("Proxy could not be created", e);
            }
        });
    }

    public static class ContainerProxy {
        private static final String CUT_CONNECTION_DOWNSTREAM = "CUT_CONNECTION_DOWNSTREAM";
        private static final String CUT_CONNECTION_UPSTREAM = "CUT_CONNECTION_UPSTREAM";
        private final Proxy toxi;
        private final String ip;
        private final int port;

        public ContainerProxy(Proxy toxi, String ip, int port) {
            this.toxi = toxi;
            this.ip = ip;
            this.port = port;
        }

        public ToxicList toxics() {
            return toxi.toxics();
        }

        public String getContainerIpAddress() {
            return ip;
        }

        public int getProxyPort() {
            return port;
        }

        /**
         * Cuts the connection by setting
         * @param shouldCutConnection
         */
        public void setConnectionCut(boolean shouldCutConnection) {
            try {
                if (shouldCutConnection) {
                    toxics().bandwidth(CUT_CONNECTION_DOWNSTREAM, ToxicDirection.DOWNSTREAM, 0);
                    toxics().bandwidth(CUT_CONNECTION_UPSTREAM, ToxicDirection.UPSTREAM, 0);
                } else {
                    toxics().get(CUT_CONNECTION_DOWNSTREAM).remove();
                    toxics().get(CUT_CONNECTION_UPSTREAM).remove();
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not control proxy", e);
            }
        }
    }
}
