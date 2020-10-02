package org.testcontainers.junit.jqwik.inheritance;

import org.testcontainers.junit.jqwik.Container;
import org.testcontainers.junit.jqwik.Testcontainers;

@Testcontainers
abstract class AbstractTestBase {

    @Container
    static RedisContainer redisPerClass = new RedisContainer();

    @Container
    RedisContainer redisPerTest = new RedisContainer();

}
