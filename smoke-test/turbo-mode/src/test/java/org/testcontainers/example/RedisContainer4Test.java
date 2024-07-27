package org.testcontainers.example;

import org.junit.jupiter.api.Test;

class RedisContainer4Test extends AbstractRedisContainer {

    @Test
    void testSimple() {
        runRedisContainer();
    }
}
