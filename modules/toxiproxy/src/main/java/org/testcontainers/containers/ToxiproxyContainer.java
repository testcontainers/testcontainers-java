package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import eu.rekawek.toxiproxy.model.ToxicList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Container for resiliency testing using <a href="https://github.com/Shopify/toxiproxy">Toxiproxy</a>.
 */
public class ToxiproxyContainer extends GenericContainer<ToxiproxyContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("shopify/toxiproxy");
    private static final String DEFAULT_TAG = "2.1.0";
    private static final int TOXIPROXY_CONTROL_PORT = 8474;
    private static final int FIRST_PROXIED_PORT = 8666;
    private static final int LAST_PROXIED_PORT = 8666 + 31;

    private ToxiproxyClient client;
    private final Map<String, ContainerProxy> proxies = new HashMap<>();
    private final AtomicInteger nextPort = new AtomicInteger(FIRST_PROXIED_PORT);

    /**
     * @deprecated use {@link ToxiproxyContainer(DockerImageName)} instead
     */
    @Deprecated
    public ToxiproxyContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public ToxiproxyContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public ToxiproxyContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

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
        client = new ToxiproxyClient(getHost(), getMappedPort(TOXIPROXY_CONTROL_PORT));
    }

    /**
     * @return Publicly exposed Toxiproxy HTTP API control port.
     */
    public int getControlPort() {
        return getMappedPort(TOXIPROXY_CONTROL_PORT);
    }

    /**
     * Obtain a {@link ContainerProxy} instance for target container that is managed by Testcontainers. The target
     * container should be routable from this <b>from this {@link ToxiproxyContainer} instance</b> (e.g. on the same
     * Docker {@link Network}).
     *
     * @param container target container
     * @param port port number on the target service that should be proxied
     * @return a {@link ContainerProxy} instance
     */
    public ContainerProxy getProxy(GenericContainer<?> container, int port) {
        return this.getProxy(container.getNetworkAliases().get(0), port);
    }

    /**
     * Obtain a {@link ContainerProxy} instance for a specific hostname and port, which can be for any host
     * that is routable <b>from this {@link ToxiproxyContainer} instance</b> (e.g. on the same
     * Docker {@link Network} or on routable from the Docker host).
     *
     * <p><em>It is expected that {@link ToxiproxyContainer#getProxy(GenericContainer, int)} will be more
     * useful in most scenarios, but this method is present to allow use of Toxiproxy in front of containers
     * or external servers that are not managed by Testcontainers.</em></p>
     *
     * @param hostname hostname of target server to be proxied
     * @param port port number on the target server that should be proxied
     * @return a {@link ContainerProxy} instance
     */
    public ContainerProxy getProxy(String hostname, int port) {
        String upstream = hostname + ":" + port;

        return proxies.computeIfAbsent(upstream, __ -> {
            try {
                final int toxiPort = nextPort.getAndIncrement();
                if (toxiPort > LAST_PROXIED_PORT) {
                    throw new IllegalStateException("Maximum number of proxies exceeded");
                }

                final Proxy proxy = client.createProxy(upstream, "0.0.0.0:" + toxiPort, upstream);
                final int mappedPort = getMappedPort(toxiPort);
                return new ContainerProxy(proxy, getHost(), mappedPort, toxiPort);
            } catch (IOException e) {
                throw new RuntimeException("Proxy could not be created", e);
            }
        });
    }

    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ContainerProxy {
        private static final String CUT_CONNECTION_DOWNSTREAM = "CUT_CONNECTION_DOWNSTREAM";
        private static final String CUT_CONNECTION_UPSTREAM = "CUT_CONNECTION_UPSTREAM";
        private final Proxy toxi;
        /**
         * The IP address that this proxy container may be reached on from the host machine.
         */
        @Getter private final String containerIpAddress;
        /**
         * The mapped port of this proxy. This is a port of the host machine. It can be used to
         * access the Toxiproxy container from the host machine.
         */
        @Getter private final int proxyPort;
        /**
         * The original (exposed) port of this proxy. This is a port of the Toxiproxy Docker
         * container. It can be used to access this container from a different Docker container
         * on the same network.
         */
        @Getter private final int originalProxyPort;
        private boolean isCurrentlyCut;

        public String getName() {
            return toxi.getName();
        }

        public ToxicList toxics() {
            return toxi.toxics();
        }

        /**
         * Cuts the connection by setting bandwidth in both directions to zero.
         * @param shouldCutConnection true if the connection should be cut, or false if it should be re-enabled
         */
        public void setConnectionCut(boolean shouldCutConnection) {
            try {
                if (shouldCutConnection) {
                    toxics().bandwidth(CUT_CONNECTION_DOWNSTREAM, ToxicDirection.DOWNSTREAM, 0);
                    toxics().bandwidth(CUT_CONNECTION_UPSTREAM, ToxicDirection.UPSTREAM, 0);
                    isCurrentlyCut = true;
                } else if (isCurrentlyCut) {
                    toxics().get(CUT_CONNECTION_DOWNSTREAM).remove();
                    toxics().get(CUT_CONNECTION_UPSTREAM).remove();
                    isCurrentlyCut = false;
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not control proxy", e);
            }
        }
    }
}
