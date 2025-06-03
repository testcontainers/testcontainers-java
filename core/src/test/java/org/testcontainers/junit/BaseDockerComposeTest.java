package org.testcontainers.junit;

import com.github.dockerjava.api.model.Network;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.TestEnvironment;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Created by rnorth on 21/05/2016.
 */
@Testcontainers
public abstract class BaseDockerComposeTest {

    protected static final int REDIS_PORT = 6379;

    protected abstract DockerComposeContainer getEnvironment();

    private List<String> existingNetworks = new ArrayList<>();

    @BeforeAll
    public static void checkVersion() {
        assumeThat(TestEnvironment.dockerApiAtLeast("1.22"))
            .as("dockerApiAtLeast(\"1.22\")")
            .isTrue();
    }

    @Test
    public void simpleTest() {
        Jedis jedis = new Jedis(
            getEnvironment().getServiceHost("redis_1", REDIS_PORT),
            getEnvironment().getServicePort("redis_1", REDIS_PORT)
        );

        jedis.incr("test");
        jedis.incr("test");
        jedis.incr("test");

        assertThat(jedis.get("test")).as("A redis instance defined in compose can be used in isolation").isEqualTo("3");
    }

    @Test
    public void secondTest() {
        // used in manual checking for cleanup in between tests
        Jedis jedis = new Jedis(
            getEnvironment().getServiceHost("redis_1", REDIS_PORT),
            getEnvironment().getServicePort("redis_1", REDIS_PORT)
        );

        jedis.incr("test");
        jedis.incr("test");
        jedis.incr("test");

        assertThat(jedis.get("test")).as("Tests use fresh container instances").isEqualTo("3");
        // if these end up using the same container one of the test methods will fail.
        // However, @Rule creates a separate DockerComposeContainer instance per test, so this just shouldn't happen
    }

    @BeforeEach
    public void captureNetworks() {
        existingNetworks.addAll(findAllNetworks());
    }

    @AfterEach
    public void verifyNoNetworks() {
        assertThat(findAllNetworks()).as("The networks").isEqualTo(existingNetworks);
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
