package org.testcontainers.junit.jupiter.inheritance;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;

public class RedisContainer extends GenericContainer<RedisContainer> {

    public RedisContainer() {
        super(DockerImageName.parse("redis:3.2.11"));
        withExposedPorts(6379);
    }

    public Jedis getJedis() {
        return new Jedis(getHost(), getMappedPort(6379));
    }
}
