package org.testcontainers.junit.jupiter.inheritance;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Restarted;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class InheritedTest1 extends AbstractTest {

    @Restarted
    private RedisContainer myRedis = new RedisContainer();

    @Test
    void step1() {
        //assertEquals(globalCounter.incrementAndGet(), redisSingleton.getJedis().incr("key").longValue());
        assertEquals(1, redisPerClass.getJedis().incr("key").longValue());
        assertEquals(1, redisPerTest.getJedis().incr("key").longValue());
        assertEquals(1, myRedis.getJedis().incr("key").longValue());
    }

    @Test
    void step2() {
        //assertEquals(globalCounter.incrementAndGet(), redisSingleton.getJedis().incr("key").longValue());
        assertEquals(2, redisPerClass.getJedis().incr("key").longValue());
        assertEquals(1, redisPerTest.getJedis().incr("key").longValue());
        assertEquals(1, myRedis.getJedis().incr("key").longValue());
    }
}
