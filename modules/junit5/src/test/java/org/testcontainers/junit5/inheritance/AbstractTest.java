package org.testcontainers.junit5.inheritance;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit5.TestcontainersExtension;
import org.testcontainers.junit5.containers.RedisContainer;

import java.util.concurrent.atomic.AtomicLong;

abstract class AbstractTest {

    static AtomicLong globalCounter = new AtomicLong(0);

    @RegisterExtension
    static TestcontainersExtension testcontainers = new TestcontainersExtension();

    protected static RedisContainer redisSingleton = testcontainers.singleton(new RedisContainer());

    protected static RedisContainer redisPerClass = testcontainers.perClass(new RedisContainer());

    protected static RedisContainer redisPerTest = testcontainers.perTest(new RedisContainer());

}


