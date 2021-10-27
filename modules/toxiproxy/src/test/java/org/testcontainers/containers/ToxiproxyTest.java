package org.testcontainers.containers;

import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.time.Duration;

import static java.lang.String.format;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

public class ToxiproxyTest {

    private static final Duration JEDIS_TIMEOUT = Duration.ofSeconds(10);

    // creatingProxy {
    // An alias that can be used to resolve the Toxiproxy container by name in the network it is connected to.
    // It can be used as a hostname of the Toxiproxy container by other containers in the same network.
    private static final String TOXIPROXY_NETWORK_ALIAS = "toxiproxy";

    // Create a common docker network so that containers can communicate
    @Rule
    public Network network = Network.newNetwork();

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:5.0.4");
    // The target container - this could be anything
    @Rule
    public GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE)
        .withExposedPorts(6379)
        .withNetwork(network);

    private static final DockerImageName TOXIPROXY_IMAGE = DockerImageName.parse("shopify/toxiproxy:2.1.0");
    // Toxiproxy container, which will be used as a TCP proxy
    @Rule
    public ToxiproxyContainer toxiproxy = new ToxiproxyContainer(TOXIPROXY_IMAGE)
        .withNetwork(network)
        .withNetworkAliases(TOXIPROXY_NETWORK_ALIAS);
    // }

    @Test
    public void testDirect() {
        final Jedis jedis = createJedis(redis.getHost(), redis.getFirstMappedPort());
        jedis.set("somekey", "somevalue");

        final String s = jedis.get("somekey");
        assertEquals("direct access to the container works OK", "somevalue", s);
    }

    @Test
    public void testLatencyViaProxy() throws IOException {
        // obtainProxyObject {
        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(redis, 6379);
        // }

        // obtainProxiedHostAndPortForHostMachine {
        final String ipAddressViaToxiproxy = proxy.getContainerIpAddress();
        final int portViaToxiproxy = proxy.getProxyPort();
        // }

        final Jedis jedis = createJedis(ipAddressViaToxiproxy, portViaToxiproxy);
        jedis.set("somekey", "somevalue");

        checkCallWithLatency(jedis, "without interference", 0, 250);

        // addingLatency {
        proxy.toxics()
            .latency("latency", ToxicDirection.DOWNSTREAM, 1_100)
            .setJitter(100);
        // from now on the connection latency should be from 1000-1200 ms.
        // }

        checkCallWithLatency(jedis, "with interference", 1_000, 1_500);
    }

    @Test
    public void testConnectionCut() {
        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(redis, 6379);
        final Jedis jedis = createJedis(proxy.getContainerIpAddress(), proxy.getProxyPort());
        jedis.set("somekey", "somevalue");

        assertEquals("access to the container works OK before cutting the connection", "somevalue", jedis.get("somekey"));

        // disableProxy {
        proxy.setConnectionCut(true);

        // for example, expect failure when the connection is cut
        assertThrows("calls fail when the connection is cut",
            JedisConnectionException.class, () -> {
                jedis.get("somekey");
            });

        proxy.setConnectionCut(false);

        // and with the connection re-established, expect success
        assertEquals("access to the container works OK after re-establishing the connection", "somevalue", jedis.get("somekey"));
        // }
    }

    @Test
    public void testMultipleProxiesCanBeCreated() {
        try (GenericContainer<?> secondRedis = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379)
            .withNetwork(network)) {

            secondRedis.start();

            final ToxiproxyContainer.ContainerProxy firstProxy = toxiproxy.getProxy(redis, 6379);
            final ToxiproxyContainer.ContainerProxy secondProxy = toxiproxy.getProxy(secondRedis, 6379);

            final Jedis firstJedis = createJedis(firstProxy.getContainerIpAddress(), firstProxy.getProxyPort());
            final Jedis secondJedis = createJedis(secondProxy.getContainerIpAddress(), secondProxy.getProxyPort());

            firstJedis.set("somekey", "somevalue");
            secondJedis.set("somekey", "somevalue");

            firstProxy.setConnectionCut(true);

            assertThrows("calls fail when the connection is cut, for only the relevant proxy",
                JedisConnectionException.class, () -> {
                    firstJedis.get("somekey");
                });

            assertEquals("access via a different proxy is OK", "somevalue", secondJedis.get("somekey"));
        }
    }

    @Test
    public void testOriginalAndMappedPorts() {
        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy("hostname", 7070);
        // obtainProxiedHostAndPortForDifferentContainer {
        final String hostViaToxiproxy = TOXIPROXY_NETWORK_ALIAS;
        final int portViaToxiproxy = proxy.getOriginalProxyPort();
        // }
        assertEquals("host is correct", TOXIPROXY_NETWORK_ALIAS, hostViaToxiproxy);
        assertEquals("original port is correct", 8666, portViaToxiproxy);

        final ToxiproxyContainer.ContainerProxy proxy1 = toxiproxy.getProxy("hostname1", 8080);
        assertEquals("original port is correct", 8667, proxy1.getOriginalProxyPort());
        assertEquals("mapped port is correct", toxiproxy.getMappedPort(proxy1.getOriginalProxyPort()), proxy1.getProxyPort());

        final ToxiproxyContainer.ContainerProxy proxy2 = toxiproxy.getProxy("hostname2", 9090);
        assertEquals("original port is correct", 8668, proxy2.getOriginalProxyPort());
        assertEquals("mapped port is correct", toxiproxy.getMappedPort(proxy2.getOriginalProxyPort()), proxy2.getProxyPort());
    }

    @Test
    public void testProxyName() {
        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy("hostname", 7070);

        assertEquals("proxy name is hostname and port", "hostname:7070", proxy.getName());
    }

    @Test
    public void testControlPort() {
        final int controlPort = toxiproxy.getControlPort();

        assertEquals("control port is mapped from port 8474", toxiproxy.getMappedPort(8474), controlPort);
    }

    private void checkCallWithLatency(Jedis jedis, final String description, int expectedMinLatency, long expectedMaxLatency) {
        final long start = System.currentTimeMillis();
        String s = jedis.get("somekey");
        final long end = System.currentTimeMillis();
        final long duration = end - start;

        assertEquals(format("access to the container %s works OK", description), "somevalue", s);
        assertTrue(format("%s there is at least %dms latency", description, expectedMinLatency), duration >= expectedMinLatency);
        assertTrue(format("%s there is no more than %dms latency", description, expectedMaxLatency), duration < expectedMaxLatency);
    }

    private static Jedis createJedis(String host, int port) {
        return new Jedis(host, port, Math.toIntExact(JEDIS_TIMEOUT.toMillis()));
    }
}
