package org.testcontainers.junit5.inheritance;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit5.containers.RedisContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InheritedTest1 extends AbstractTest {

    RedisContainer myRedis = testcontainers.perTest(new RedisContainer());

    @Test
    void step1() {
        assertEquals(globalCounter.incrementAndGet(), redisSingleton.getJedis().incr("key").longValue());
        assertEquals(1, redisPerClass.getJedis().incr("key").longValue());
        assertEquals(1, redisPerTest.getJedis().incr("key").longValue());
        assertEquals(1, myRedis.getJedis().incr("key").longValue());
    }

    @Test
    void step2() {
        assertEquals(globalCounter.incrementAndGet(), redisSingleton.getJedis().incr("key").longValue());
        assertEquals(2, redisPerClass.getJedis().incr("key").longValue());
        assertEquals(1, redisPerTest.getJedis().incr("key").longValue());
        assertEquals(1, myRedis.getJedis().incr("key").longValue());
    }
}
