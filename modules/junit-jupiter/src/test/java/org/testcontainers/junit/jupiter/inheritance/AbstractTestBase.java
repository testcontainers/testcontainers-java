package org.testcontainers.junit.jupiter.inheritance;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
abstract class AbstractTestBase {

    @Container
    static RedisContainer redisPerClass = new RedisContainer();

    @Container
    RedisContainer redisPerTest = new RedisContainer();

}
