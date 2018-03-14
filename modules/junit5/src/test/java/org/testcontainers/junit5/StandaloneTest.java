package org.testcontainers.junit5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit5.containers.RedisContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StandaloneTest {

    @RegisterExtension
    static TestcontainersExtension testcontainers = new TestcontainersExtension();

    static RedisContainer redisPerClass = testcontainers.perClass(new RedisContainer());

    RedisContainer redisPerTest = testcontainers.perTest(new RedisContainer());

    @Test
    void step1() {
        assertEquals(1, redisPerClass.getJedis().incr("key").longValue());
        assertEquals(1, redisPerTest.getJedis().incr("key").longValue());
    }

    @Test
    void step2() {
        assertEquals(2, redisPerClass.getJedis().incr("key").longValue());
        assertEquals(1, redisPerTest.getJedis().incr("key").longValue());
    }

    @Test
    void step3() {
        assertEquals(3, redisPerClass.getJedis().incr("key").longValue());
        assertEquals(1, redisPerTest.getJedis().incr("key").longValue());
    }

    @Test
    void step4() {
        assertEquals(4, redisPerClass.getJedis().incr("key").longValue());
        assertEquals(1, redisPerTest.getJedis().incr("key").longValue());
    }
}
