package org.testcontainers.junit;

import com.github.dockerjava.api.model.Network;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.TestEnvironment;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThat;

public class DockerComposeV2Test {

    @Rule
    public DockerComposeContainer environment = new DockerComposeContainer(
        new File("src/test/resources/v2-compose-test.yml")
    )
        .withExposedService("redis-1", REDIS_PORT);

    protected static final int REDIS_PORT = 6379;

    private List<String> existingNetworks = new ArrayList<>();

    @BeforeClass
    public static void checkVersion() {
        Assume.assumeTrue(TestEnvironment.dockerApiAtLeast("1.22"));
        Assume.assumeTrue(Boolean.parseBoolean(System.getenv("TESTCONTAINERS_COMPOSEV2_ENABLE")));
    }

    @Test
    public void simpleTest() {
        Jedis jedis = new Jedis(
            this.environment.getServiceHost("redis-1", REDIS_PORT),
            this.environment.getServicePort("redis-1", REDIS_PORT)
        );

        jedis.incr("test");
        jedis.incr("test");
        jedis.incr("test");

        assertEquals("A redis instance defined in compose can be used in isolation", "3", jedis.get("test"));
    }

    @Test
    public void secondTest() {
        // used in manual checking for cleanup in between tests
        Jedis jedis = new Jedis(
            this.environment.getServiceHost("redis-1", REDIS_PORT),
            this.environment.getServicePort("redis-1", REDIS_PORT)
        );

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
        return DockerClientFactory
            .instance()
            .client()
            .listNetworksCmd()
            .exec()
            .stream()
            .map(Network::getName)
            .sorted()
            .collect(Collectors.toList());
    }
}
