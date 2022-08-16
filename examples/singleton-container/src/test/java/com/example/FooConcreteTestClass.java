package com.example;

import com.example.cache.Cache;
import com.example.cache.RedisBackedCache;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class FooConcreteTestClass extends AbstractIntegrationTest {

    private Cache cache;

    @Before
    public void setUp() {
        Jedis jedis = new Jedis(redis.getHost(), redis.getMappedPort(6379));

        cache = new RedisBackedCache(jedis, "foo");
    }

    @Test
    public void testInsertValue() {
        cache.put("foo", "FOO");
        Optional<String> foundObject = cache.get("foo", String.class);

        assertThat(foundObject).as("When inserting an object into the cache, it can be retrieved").isPresent();
        assertThat(foundObject).as("When accessing the value of a retrieved object, the value must be the same").contains("FOO");
    }
}
