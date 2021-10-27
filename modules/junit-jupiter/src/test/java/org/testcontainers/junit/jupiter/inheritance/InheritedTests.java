package org.testcontainers.junit.jupiter.inheritance;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InheritedTests extends AbstractTestBase {

    @Container
    private RedisContainer myRedis = new RedisContainer();

    @Test
    void step1() {
        assertEquals(1, redisPerClass.getJedis().incr("key").longValue());
        assertEquals(1, redisPerTest.getJedis().incr("key").longValue());
        assertEquals(1, myRedis.getJedis().incr("key").longValue());
    }

    @Test
    void step2() {
        assertEquals(2, redisPerClass.getJedis().incr("key").longValue());
        assertEquals(1, redisPerTest.getJedis().incr("key").longValue());
        assertEquals(1, myRedis.getJedis().incr("key").longValue());
    }
}
