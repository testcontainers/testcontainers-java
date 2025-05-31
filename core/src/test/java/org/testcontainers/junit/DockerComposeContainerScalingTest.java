package org.testcontainers.junit;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit4.TestcontainersRule;
import org.testcontainers.utility.TestEnvironment;
import redis.clients.jedis.Jedis;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by rnorth on 08/08/2015.
 */
public class DockerComposeContainerScalingTest {

    private static final int REDIS_PORT = 6379;

    private Jedis[] clients = new Jedis[3];

    @BeforeClass
    public static void checkVersion() {
        Assume.assumeTrue(TestEnvironment.dockerApiAtLeast("1.22"));
    }

    @Rule
    public TestcontainersRule<DockerComposeContainer> environment = new TestcontainersRule<>(
        new DockerComposeContainer(new File("src/test/resources/scaled-compose-test.yml"))
            .withScaledService("redis", 3)
            .withExposedService("redis", REDIS_PORT) // implicit '_1'
            .withExposedService("redis_2", REDIS_PORT) // explicit service index
            .withExposedService("redis", 3, REDIS_PORT) // explicit service index via parameter
    );

    @Before
    public void setupClients() {
        for (int i = 0; i < 3; i++) {
            String name = String.format("redis_%d", i + 1);

            clients[i] =
                new Jedis(
                    environment.get().getServiceHost(name, REDIS_PORT),
                    environment.get().getServicePort(name, REDIS_PORT)
                );
        }
    }

    @Test
    public void simpleTest() {
        for (int i = 0; i < 3; i++) {
            clients[i].incr("somekey");

            assertThat(clients[i].get("somekey")).as("Each redis instance is separate").isEqualTo("1");
        }
    }
}
