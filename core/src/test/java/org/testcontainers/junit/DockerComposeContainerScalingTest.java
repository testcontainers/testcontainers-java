package org.testcontainers.junit;

import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.TestEnvironment;
import redis.clients.jedis.Jedis;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by rnorth on 08/08/2015.
 */
class DockerComposeContainerScalingTest {

    private static final int REDIS_PORT = 6379;

    private Jedis[] clients = new Jedis[3];

    @BeforeAll
    public static void checkVersion() {
        Assumptions.assumeThat(TestEnvironment.dockerApiAtLeast("1.22")).isTrue();
    }

    @AutoClose
    public DockerComposeContainer environment = new DockerComposeContainer(
        new File("src/test/resources/scaled-compose-test.yml")
    )
        .withScaledService("redis", 3)
        .withExposedService("redis", REDIS_PORT) // implicit '_1'
        .withExposedService("redis_2", REDIS_PORT) // explicit service index
        .withExposedService("redis", 3, REDIS_PORT); // explicit service index via parameter

    @BeforeEach
    public void setupClients() {
        this.environment.start();
        for (int i = 0; i < 3; i++) {
            String name = String.format("redis_%d", i + 1);

            clients[i] =
                new Jedis(environment.getServiceHost(name, REDIS_PORT), environment.getServicePort(name, REDIS_PORT));
        }
    }

    @Test
    void simpleTest() {
        for (int i = 0; i < 3; i++) {
            clients[i].incr("somekey");

            assertThat(clients[i].get("somekey")).as("Each redis instance is separate").isEqualTo("1");
        }
    }
}
