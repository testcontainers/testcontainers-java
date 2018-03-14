package org.testcontainers.junit5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.DockerComposeContainer;
import redis.clients.jedis.Jedis;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DockerComposeTest {

    @RegisterExtension
    static TestcontainersExtension testcontainers = new TestcontainersExtension();

    static DockerComposeContainer environment = testcontainers.perClass(
        new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withLocalCompose(true)
            .withExposedService("redis", 6379)
    );

    @Test
    void step1() {
        assertEquals(1, new Jedis(environment.getServiceHost("redis_1", 6379), environment.getServicePort("redis_1", 6379)).incr("key").longValue());
    }

    @Test
    void step2() {
        assertEquals(2, new Jedis(environment.getServiceHost("redis_1", 6379), environment.getServicePort("redis_1", 6379)).incr("key").longValue());
    }
}
