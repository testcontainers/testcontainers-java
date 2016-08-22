package org.testcontainers.junit;

import com.github.dockerjava.api.model.Network;
import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.TestEnvironment;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThat;

/**
 * Created by rnorth on 21/05/2016.
 */
public abstract class BaseDockerComposeTest {

    protected static final int REDIS_PORT = 6379;

    protected abstract DockerComposeContainer getEnvironment();

    private List<String> existingNetworks = new ArrayList<>();

    @BeforeClass
    public static void checkVersion() {
        Assume.assumeTrue(TestEnvironment.dockerApiAtLeast("1.22"));
    }

    @Test
    public void simpleTest() {
        Jedis jedis = new Jedis(getEnvironment().getServiceHost("redis_1", REDIS_PORT), getEnvironment().getServicePort("redis_1", REDIS_PORT));

        // TODO: remove following resolution of #160
        Unreliables.retryUntilSuccess(10, TimeUnit.SECONDS, getLivenessCheck(jedis));

        jedis.incr("test");
        jedis.incr("test");
        jedis.incr("test");

        assertEquals("A redis instance defined in compose can be used in isolation", "3", jedis.get("test"));
    }

    @Test
    public void secondTest() {
        // used in manual checking for cleanup in between tests
        Jedis jedis = new Jedis(getEnvironment().getServiceHost("redis_1", REDIS_PORT), getEnvironment().getServicePort("redis_1", REDIS_PORT));

        // TODO: remove following resolution of #160
        Unreliables.retryUntilSuccess(10, TimeUnit.SECONDS, getLivenessCheck(jedis));

        jedis.incr("test");
        jedis.incr("test");
        jedis.incr("test");

        assertEquals("Tests use fresh container instances", "3", jedis.get("test"));
        // if these end up using the same container one of the test methods will fail.
        // However, @Rule creates a separate DockerComposeContainer instance per test, so this just shouldn't happen
    }

    @Before
    public void captureNetworks() {
      existingNetworks.addAll(findAllNetworks());
    }

    @After
    public void verifyNoNetworks() {
      assertThat("The networks", findAllNetworks(), is(existingNetworks));
    }

    private List<String> findAllNetworks() {
      return DockerClientFactory.instance().client().listNetworksCmd().exec().stream()
        .map(Network::getName)
        .sorted()
        .collect(Collectors.toList());
    }

    @NotNull
    private Callable<Boolean> getLivenessCheck(Jedis jedis) {
        return () -> {
            jedis.connect();
            jedis.ping();
            return true;
        };
    }
}
