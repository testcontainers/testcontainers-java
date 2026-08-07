package org.testcontainers.memcached;

import net.spy.memcached.MemcachedClient;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MemcachedContainerTest {

    private static final DockerImageName MEMCACHED_IMAGE = DockerImageName.parse("memcached:1.6-alpine");

    @Test
    void testSimple() throws Exception {
        try (
            // creatingContainer {
            MemcachedContainer memcached = new MemcachedContainer(MEMCACHED_IMAGE)
            // }
        ) {
            memcached.start();

            // connectAndOperate {
            MemcachedClient client = new MemcachedClient(
                new InetSocketAddress(memcached.getHost(), memcached.getMappedPort(11211))
            );
            try {
                client.set("hello", 60, "world").get(5, TimeUnit.SECONDS);
                assertThat(client.get("hello")).isEqualTo("world");
            } finally {
                client.shutdown();
            }
            // }
        }
    }
}
