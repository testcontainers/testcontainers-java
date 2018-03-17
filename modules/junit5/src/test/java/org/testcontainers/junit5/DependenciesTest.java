package org.testcontainers.junit5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.junit5.containers.RedisContainer;

class DependenciesTest {

    @RegisterExtension
    static TestcontainersExtension testcontainers = new TestcontainersExtension();

    RedisContainer redis = testcontainers.perTest(
        new RedisContainer()
            .withNetwork(Network.SHARED)
            .withNetworkAliases("redis")
    );

    GenericContainer cli1 = testcontainers.perTest(
        new GenericContainer<>("redis:3.2.11")
            .withNetwork(Network.SHARED)
            .withNetworkAliases("cli1")
            .dependsOn(redis)
            .withCommand("redis-cli", "-h", redis.getNetworkAliases().get(0), "ping")
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
    );

    GenericContainer cli2 = testcontainers.perTest(
        new GenericContainer<>("redis:3.2.11")
            .withNetwork(Network.SHARED)
            .withNetworkAliases("cli2")
            .dependsOn(cli1)
            .withCommand("redis-cli", "-h", redis.getNetworkAliases().get(0), "ping")
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
    );

    GenericContainer cli3 = testcontainers.perTest(
        new GenericContainer<>("redis:3.2.11")
            .withNetwork(Network.SHARED)
            .withNetworkAliases("cli3")
            .dependsOn(cli1, cli2)
            .withCommand("redis-cli", "-h", redis.getNetworkAliases().get(0), "ping")
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
    );

    GenericContainer cli4 = testcontainers.perTest(
        new GenericContainer<>("redis:3.2.11")
            .withNetwork(Network.SHARED)
            .withNetworkAliases("cli4")
            .dependsOn(cli1)
            .withCommand("redis-cli", "-h", redis.getNetworkAliases().get(0), "ping")
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
    );

    @Test
    void testStartupOrder() {
    }
}
