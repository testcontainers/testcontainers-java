package org.testcontainers.junit;

import com.github.dockerjava.api.model.Network;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.utility.TestEnvironment;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseComposeTest {

    protected static final int REDIS_PORT = 6379;

    protected abstract ComposeContainer getEnvironment();

    private static final String COMPOSE_PROJECT_LABEL = "com.docker.compose.project";

    private List<String> existingNetworks = new ArrayList<>();

    private String composeProject;

    @BeforeAll
    public static void checkVersion() {
        Assumptions.assumeTrue(TestEnvironment.dockerApiAtLeast("1.22"));
    }

    @Test
    void simpleTest() {
        Jedis jedis = new Jedis(
            getEnvironment().getServiceHost("redis-1", REDIS_PORT),
            getEnvironment().getServicePort("redis-1", REDIS_PORT)
        );

        jedis.incr("test");
        jedis.incr("test");
        jedis.incr("test");

        assertThat(jedis.get("test")).as("A redis instance defined in compose can be used in isolation").isEqualTo("3");
    }

    @Test
    void secondTest() {
        // used in manual checking for cleanup in between tests
        Jedis jedis = new Jedis(
            getEnvironment().getServiceHost("redis-1", REDIS_PORT),
            getEnvironment().getServicePort("redis-1", REDIS_PORT)
        );

        jedis.incr("test");
        jedis.incr("test");
        jedis.incr("test");

        assertThat(jedis.get("test")).as("Tests use fresh container instances").isEqualTo("3");
        // if these end up using the same container one of the test methods will fail.
        // However, @Rule creates a separate ComposeContainer instance per test, so this just shouldn't happen
    }

    @BeforeEach
    public void captureNetworks() {
        composeProject =
            getEnvironment()
                .getContainerByServiceName("redis-1")
                .map(container -> container.getContainerInfo().getConfig().getLabels().get(COMPOSE_PROJECT_LABEL))
                .orElseThrow(() -> new IllegalStateException("Compose service redis-1 is not running"));
        existingNetworks.addAll(findComposeProjectNetworks());
        assertThat(existingNetworks).as("The networks of compose project %s", composeProject).isNotEmpty();
    }

    @AfterEach
    public void verifyNoNetworks() {
        assertThat(findComposeProjectNetworks()).as("The networks").isEqualTo(existingNetworks);
    }

    /**
     * Only look at the networks of this test's compose project. Core tests run in parallel forks against the same
     * Docker daemon, so other tests create and remove networks while this test runs; comparing the daemon-wide network
     * list made this check flaky.
     */
    private List<String> findComposeProjectNetworks() {
        return DockerClientFactory
            .instance()
            .client()
            .listNetworksCmd()
            .withFilter("label", Collections.singletonList(COMPOSE_PROJECT_LABEL + "=" + composeProject))
            .exec()
            .stream()
            .map(Network::getName)
            .sorted()
            .collect(Collectors.toList());
    }
}
