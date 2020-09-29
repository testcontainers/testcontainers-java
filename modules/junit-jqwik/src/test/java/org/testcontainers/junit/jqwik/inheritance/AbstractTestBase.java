package org.testcontainers.junit.jqwik.inheritance;

import org.testcontainers.junit.jqwik.TestContainer;
import org.testcontainers.junit.jqwik.Testcontainers;

@Testcontainers
abstract class AbstractTestBase {

    @TestContainer
    static RedisContainer redisPerClass = new RedisContainer();

    @TestContainer
    RedisContainer redisPerTest = new RedisContainer();

}
