package org.testcontainers.containers;

import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.Rule;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;

import static java.lang.String.format;
import static org.rnorth.visibleassertions.VisibleAssertions.*;

public class ToxiproxyTest {

    // creatingProxy {
    // Create a common docker network so that containers can communicate
    @Rule
    public Network network = Network.newNetwork();

    // the target container - this could be anything
    @Rule
    public GenericContainer redis = new GenericContainer("redis:5.0.4")
        .withExposedPorts(6379)
        .withNetwork(network);

    // Toxiproxy container, which will be used as a TCP proxy
    @Rule
    public ToxiproxyContainer toxiproxy = new ToxiproxyContainer()
        .withNetwork(network);
    // }

    @Test
    public void testDirect() {
        final Jedis jedis = new Jedis(redis.getContainerIpAddress(), redis.getFirstMappedPort());
        jedis.set("somekey", "somevalue");

        final String s = jedis.get("somekey");
        assertEquals("direct access to the container works OK", "somevalue", s);
    }

    @Test
    public void testLatencyViaProxy() throws IOException {
        // obtainProxyObject {
        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(redis, 6379);
        // }

        // obtainProxiedHostAndPort {
        final String ipAddressViaToxiproxy = proxy.getContainerIpAddress();
        final int portViaToxiproxy = proxy.getProxyPort();
        // }

        final Jedis jedis = new Jedis(ipAddressViaToxiproxy, portViaToxiproxy);
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
        final Jedis jedis = new Jedis(proxy.getContainerIpAddress(), proxy.getProxyPort());
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
        try (GenericContainer secondRedis = new GenericContainer("redis:5.0.4")
            .withExposedPorts(6379)
            .withNetwork(network)) {

            secondRedis.start();

            final ToxiproxyContainer.ContainerProxy firstProxy = toxiproxy.getProxy(redis, 6379);
            final ToxiproxyContainer.ContainerProxy secondProxy = toxiproxy.getProxy(secondRedis, 6379);

            final Jedis firstJedis = new Jedis(firstProxy.getContainerIpAddress(), firstProxy.getProxyPort());
            final Jedis secondJedis = new Jedis(secondProxy.getContainerIpAddress(), secondProxy.getProxyPort());

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

    private void checkCallWithLatency(Jedis jedis, final String description, int expectedMinLatency, long expectedMaxLatency) {
        final long start = System.currentTimeMillis();
        String s = jedis.get("somekey");
        final long end = System.currentTimeMillis();
        final long duration = end - start;

        assertEquals(format("access to the container %s works OK", description), "somevalue", s);
        assertTrue(format("%s there is at least %dms latency", description, expectedMinLatency), duration >= expectedMinLatency);
        assertTrue(format("%s there is no more than %dms latency", description, expectedMaxLatency), duration < expectedMaxLatency);
    }
}
