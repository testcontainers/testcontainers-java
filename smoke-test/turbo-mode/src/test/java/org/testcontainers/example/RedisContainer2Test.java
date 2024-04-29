package org.testcontainers.example;

import org.junit.jupiter.api.Test;

class RedisContainer2Test extends AbstractRedisContainer {

    @Test
    void testSimple() {
        runRedisContainer();
    }
}
