package org.testcontainers.containers;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.Rule;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class ToxiproxyTest {

    private static final Duration JEDIS_TIMEOUT = Duration.ofSeconds(10);

    // spotless:off
    // creatingProxy {
    // Create a common docker network so that containers can communicate
    @Rule
    public Network network = Network.newNetwork();

    // The target container - this could be anything
    @Rule
    public GenericContainer<?> redis = new GenericContainer<>("redis:6-alpine")
        .withExposedPorts(6379)
        .withNetwork(network)
        .withNetworkAliases("redis");

    // Toxiproxy container, which will be used as a TCP proxy
    @Rule
    public ToxiproxyContainer toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
        .withNetwork(network);
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
        final ToxiproxyClient toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
        final Proxy proxy = toxiproxyClient.createProxy("redis", "0.0.0.0:8666", "redis:6379");
        // }

        // obtainProxiedHostAndPortForHostMachine {
        final String ipAddressViaToxiproxy = toxiproxy.getHost();
        final int portViaToxiproxy = toxiproxy.getMappedPort(8666);
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
    public void testConnectionCut() throws IOException {
        final ToxiproxyClient toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
        final Proxy proxy = toxiproxyClient.createProxy("redis", "0.0.0.0:8666", "redis:6379");
        final Jedis jedis = createJedis(toxiproxy.getHost(), toxiproxy.getMappedPort(8666));
        jedis.set("somekey", "somevalue");

        assertThat(jedis.get("somekey"))
            .as("access to the container works OK before cutting the connection")
            .isEqualTo("somevalue");

        // disableProxy {
        proxy.toxics().bandwidth("CUT_CONNECTION_DOWNSTREAM", ToxicDirection.DOWNSTREAM, 0);
        proxy.toxics().bandwidth("CUT_CONNECTION_UPSTREAM", ToxicDirection.UPSTREAM, 0);

        // for example, expect failure when the connection is cut
        assertThat(
            catchThrowable(() -> {
                jedis.get("somekey");
            })
        )
            .as("calls fail when the connection is cut")
            .isInstanceOf(JedisConnectionException.class);

        proxy.toxics().get("CUT_CONNECTION_DOWNSTREAM").remove();
        proxy.toxics().get("CUT_CONNECTION_UPSTREAM").remove();

        jedis.close();
        // and with the connection re-established, expect success
        assertThat(jedis.get("somekey"))
            .as("access to the container works OK after re-establishing the connection")
            .isEqualTo("somevalue");
        // }
    }

    @Test
    public void testMultipleProxiesCanBeCreated() throws IOException {
        try (
            GenericContainer<?> secondRedis = new GenericContainer<>("redis:6-alpine")
                .withExposedPorts(6379)
                .withNetwork(network)
                .withNetworkAliases("redis2")
        ) {
            secondRedis.start();

            final ToxiproxyClient toxiproxyClient = new ToxiproxyClient(
                toxiproxy.getHost(),
                toxiproxy.getControlPort()
            );
            final Proxy firstProxy = toxiproxyClient.createProxy("redis1", "0.0.0.0:8666", "redis:6379");
            toxiproxyClient.createProxy("redis2", "0.0.0.0:8667", "redis2:6379");

            final Jedis firstJedis = createJedis(toxiproxy.getHost(), toxiproxy.getMappedPort(8666));
            final Jedis secondJedis = createJedis(toxiproxy.getHost(), toxiproxy.getMappedPort(8667));

            firstJedis.set("somekey", "somevalue");
            secondJedis.set("somekey", "somevalue");

            firstProxy.toxics().bandwidth("CUT_CONNECTION_DOWNSTREAM", ToxicDirection.DOWNSTREAM, 0);
            firstProxy.toxics().bandwidth("CUT_CONNECTION_UPSTREAM", ToxicDirection.UPSTREAM, 0);

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

        final int portViaToxiproxy = proxy.getOriginalProxyPort();
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

    private void checkCallWithLatency(
        Jedis jedis,
        final String description,
        int expectedMinLatency,
        long expectedMaxLatency
    ) {
        final long start = System.nanoTime();
        String s = jedis.get("somekey");
        final long end = System.nanoTime();
        final long duration = TimeUnit.NANOSECONDS.toMillis(end - start);

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
