package org.testcontainers.toxiproxy;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for Toxiproxy.
 * <p>
 * Supported images: {@code ghcr.io/shopify/toxiproxy}, {@code shopify/toxiproxy}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>HTTP: 8474</li>
 *     <li>Proxied Ports: 8666-8697</li>
 * </ul>
 */
public class ToxiproxyContainer extends GenericContainer<ToxiproxyContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("shopify/toxiproxy");

    private static final DockerImageName GHCR_IMAGE_NAME = DockerImageName.parse("ghcr.io/shopify/toxiproxy");

    private static final int TOXIPROXY_CONTROL_PORT = 8474;

    private static final int FIRST_PROXIED_PORT = 8666;

    private static final int LAST_PROXIED_PORT = 8666 + 31;

    public ToxiproxyContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public ToxiproxyContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, GHCR_IMAGE_NAME);

        addExposedPorts(TOXIPROXY_CONTROL_PORT);
        setWaitStrategy(new HttpWaitStrategy().forPath("/version").forPort(TOXIPROXY_CONTROL_PORT));

        // allow up to 32 ports to be proxied (arbitrary value). Here we make the ports exposed; whether or not
        //  Toxiproxy will listen is controlled at runtime using getProxy(...)
        for (int i = FIRST_PROXIED_PORT; i <= LAST_PROXIED_PORT; i++) {
            addExposedPort(i);
        }
    }

    /**
     * @return Publicly exposed Toxiproxy HTTP API control port.
     */
    public int getControlPort() {
        return getMappedPort(TOXIPROXY_CONTROL_PORT);
    }
}
