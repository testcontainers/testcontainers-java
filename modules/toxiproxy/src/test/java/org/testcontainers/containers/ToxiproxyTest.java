package org.testcontainers.containers;

import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class ToxiproxyTest {

    private static final Duration JEDIS_TIMEOUT = Duration.ofSeconds(10);

    // spotless:off
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
    // spotless:on

    @Test
    public void testDirect() {
        final Jedis jedis = createJedis(redis.getHost(), redis.getFirstMappedPort());
        jedis.set("somekey", "somevalue");

        final String s = jedis.get("somekey");
        assertThat(s).as("direct access to the container works OK").isEqualTo("somevalue");
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

        // spotless:off
        // addingLatency {
        proxy.toxics()
            .latency("latency", ToxicDirection.DOWNSTREAM, 1_100)
            .setJitter(100);
        // from now on the connection latency should be from 1000-1200 ms.
        // }
        // spotless:on

        checkCallWithLatency(jedis, "with interference", 1_000, 1_500);
    }

    @Test
    public void testConnectionCut() {
        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(redis, 6379);
        final Jedis jedis = createJedis(proxy.getContainerIpAddress(), proxy.getProxyPort());
        jedis.set("somekey", "somevalue");

        assertThat(jedis.get("somekey"))
            .as("access to the container works OK before cutting the connection")
            .isEqualTo("somevalue");

        // disableProxy {
        proxy.setConnectionCut(true);

        // for example, expect failure when the connection is cut
        assertThat(
            catchThrowable(() -> {
                jedis.get("somekey");
            })
        )
            .as("calls fail when the connection is cut")
            .isInstanceOf(JedisConnectionException.class);

        proxy.setConnectionCut(false);

        // and with the connection re-established, expect success
        assertThat(jedis.get("somekey"))
            .as("access to the container works OK after re-establishing the connection")
            .isEqualTo("somevalue");
        // }
    }

    @Test
    public void testMultipleProxiesCanBeCreated() {
        try (
            GenericContainer<?> secondRedis = new GenericContainer<>(REDIS_IMAGE)
                .withExposedPorts(6379)
                .withNetwork(network)
        ) {
            secondRedis.start();

            final ToxiproxyContainer.ContainerProxy firstProxy = toxiproxy.getProxy(redis, 6379);
            final ToxiproxyContainer.ContainerProxy secondProxy = toxiproxy.getProxy(secondRedis, 6379);

            final Jedis firstJedis = createJedis(firstProxy.getContainerIpAddress(), firstProxy.getProxyPort());
            final Jedis secondJedis = createJedis(secondProxy.getContainerIpAddress(), secondProxy.getProxyPort());

            firstJedis.set("somekey", "somevalue");
            secondJedis.set("somekey", "somevalue");

            firstProxy.setConnectionCut(true);

            assertThat(
                catchThrowable(() -> {
                    firstJedis.get("somekey");
                })
            )
                .as("calls fail when the connection is cut, for only the relevant proxy")
                .isInstanceOf(JedisConnectionException.class);

            assertThat(secondJedis.get("somekey")).as("access via a different proxy is OK").isEqualTo("somevalue");
        }
    }

    @Test
    public void testOriginalAndMappedPorts() {
        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy("hostname", 7070);
        // obtainProxiedHostAndPortForDifferentContainer {
        final String hostViaToxiproxy = TOXIPROXY_NETWORK_ALIAS;
        final int portViaToxiproxy = proxy.getOriginalProxyPort();
        // }
        assertThat(hostViaToxiproxy).as("host is correct").isEqualTo(TOXIPROXY_NETWORK_ALIAS);
        assertThat(portViaToxiproxy).as("original port is correct").isEqualTo(8666);

        final ToxiproxyContainer.ContainerProxy proxy1 = toxiproxy.getProxy("hostname1", 8080);
        assertThat(proxy1.getOriginalProxyPort()).as("original port is correct").isEqualTo(8667);
        assertThat(proxy1.getProxyPort())
            .as("mapped port is correct")
            .isEqualTo(toxiproxy.getMappedPort(proxy1.getOriginalProxyPort()));

        final ToxiproxyContainer.ContainerProxy proxy2 = toxiproxy.getProxy("hostname2", 9090);
        assertThat(proxy2.getOriginalProxyPort()).as("original port is correct").isEqualTo(8668);
        assertThat(proxy2.getProxyPort())
            .as("mapped port is correct")
            .isEqualTo(toxiproxy.getMappedPort(proxy2.getOriginalProxyPort()));
    }

    @Test
    public void testProxyName() {
        final ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy("hostname", 7070);

        assertThat(proxy.getName()).as("proxy name is hostname and port").isEqualTo("hostname:7070");
    }

    @Test
    public void testControlPort() {
        final int controlPort = toxiproxy.getControlPort();

        assertThat(controlPort).as("control port is mapped from port 8474").isEqualTo(toxiproxy.getMappedPort(8474));
    }

    private void checkCallWithLatency(
        Jedis jedis,
        final String description,
        int expectedMinLatency,
        long expectedMaxLatency
    ) {
        final long start = System.currentTimeMillis();
        String s = jedis.get("somekey");
        final long end = System.currentTimeMillis();
        final long duration = end - start;

        assertThat(s).as(String.format("access to the container %s works OK", description)).isEqualTo("somevalue");
        assertThat(duration >= expectedMinLatency)
            .as(String.format("%s there is at least %dms latency", description, expectedMinLatency))
            .isTrue();
        assertThat(duration < expectedMaxLatency)
            .as(String.format("%s there is no more than %dms latency", description, expectedMaxLatency))
            .isTrue();
    }

    private static Jedis createJedis(String host, int port) {
        return new Jedis(host, port, Math.toIntExact(JEDIS_TIMEOUT.toMillis()));
    }
}
