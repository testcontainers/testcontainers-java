package org.testcontainers.example;

import org.junit.jupiter.api.Test;

class RedisContainer5Test extends AbstractRedisContainer {

    @Test
    public void testSimple() {
        runRedisContainer();
    }
}
