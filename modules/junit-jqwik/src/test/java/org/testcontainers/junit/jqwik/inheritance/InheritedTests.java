package org.testcontainers.junit.jqwik.inheritance;

import net.jqwik.api.Example;
import org.testcontainers.junit.jqwik.Container;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InheritedTests extends AbstractTestBase {

    @Container
    private RedisContainer myRedis = new RedisContainer();

    @Example
    void step1() {
        assertEquals(1, redisPerClass.getJedis().incr("key").longValue());
        assertEquals(1, redisPerTest.getJedis().incr("key").longValue());
        assertEquals(1, myRedis.getJedis().incr("key").longValue());
    }

    @Example
    void step2() {
        assertEquals(2, redisPerClass.getJedis().incr("key").longValue());
        assertEquals(1, redisPerTest.getJedis().incr("key").longValue());
        assertEquals(1, myRedis.getJedis().incr("key").longValue());
    }
}
