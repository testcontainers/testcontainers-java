package org.testcontainers.junit.jupiter.inheritance;

import org.testcontainers.junit.jupiter.Restarted;
import org.testcontainers.junit.jupiter.Shared;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.atomic.AtomicLong;

@Testcontainers
abstract class AbstractTest {

    static AtomicLong globalCounter = new AtomicLong(0);

    @Shared
    RedisContainer redisPerClass = new RedisContainer();

    @Restarted
    RedisContainer redisPerTest = new RedisContainer();

}
