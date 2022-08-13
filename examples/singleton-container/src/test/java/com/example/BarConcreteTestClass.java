package com.example;

import com.example.cache.Cache;
import com.example.cache.RedisBackedCache;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class BarConcreteTestClass extends AbstractIntegrationTest {

    private Cache cache;

    @Before
    public void setUp() {
        Jedis jedis = new Jedis(redis.getHost(), redis.getMappedPort(6379));

        cache = new RedisBackedCache(jedis, "bar");
    }

    @Test
    public void testInsertValue() {
        cache.put("bar", "BAR");
        Optional<String> foundObject = cache.get("bar", String.class);

        assertThat(foundObject).as("When inserting an object into the cache, it can be retrieved").isPresent();
        assertThat(foundObject).as("When accessing the value of a retrieved object, the value must be the same").contains("BAR");
    }

}
