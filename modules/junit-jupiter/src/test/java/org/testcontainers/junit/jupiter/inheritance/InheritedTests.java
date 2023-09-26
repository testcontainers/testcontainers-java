package org.testcontainers.junit.jupiter.inheritance;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;

import static org.assertj.core.api.Assertions.assertThat;

class InheritedTests extends AbstractTestBase {

    @Container
    private RedisContainer myRedis = new RedisContainer();

    @Test
    void step1() {
        assertThat(redisPerClass.getJedis().incr("key")).isEqualTo(1);
        assertThat(redisPerTest.getJedis().incr("key")).isEqualTo(1);
        assertThat(myRedis.getJedis().incr("key")).isEqualTo(1);
    }

    @Test
    void step2() {
        assertThat(redisPerClass.getJedis().incr("key")).isEqualTo(2);
        assertThat(redisPerTest.getJedis().incr("key")).isEqualTo(1);
        assertThat(myRedis.getJedis().incr("key")).isEqualTo(1);
    }
}
