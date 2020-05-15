package org.testcontainers.junit.jupiter.inheritance;

import org.testcontainers.containers.GenericContainer;
import redis.clients.jedis.Jedis;

public class RedisContainer extends GenericContainer<RedisContainer> {

    public RedisContainer() {
        this("redis:3.2.11");
    }

    public RedisContainer(String dockerImageName) {
        super(dockerImageName);
        withExposedPorts(6379);
    }

    public Jedis getJedis() {
        return new Jedis(getHost(), getMappedPort(6379));
    }
}
