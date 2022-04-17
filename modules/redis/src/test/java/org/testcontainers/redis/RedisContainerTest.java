package org.testcontainers.redis;

import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.Jedis;

public class RedisContainerTest {

    @Test
    public void connectTest() {
        final int redisPort = 6666;
        RedisContainer redisContainer = new RedisContainer().withPort(redisPort);
        redisContainer.start();

        Jedis jedis = new Jedis(redisContainer.getHost(), redisPort);
        String pong = jedis.ping();
        Assert.assertEquals("PONG", pong);
    }

}
