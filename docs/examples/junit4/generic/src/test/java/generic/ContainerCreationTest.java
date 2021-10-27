package generic;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.Assert.assertTrue;

public class ContainerCreationTest {

    // simple {
    public static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:3.0.2");
    // }

    // simple {
    @ClassRule
    public static GenericContainer<?> redis =
        new GenericContainer<>(REDIS_IMAGE)
                .withExposedPorts(6379);
    // }

    public static final DockerImageName ALPINE_IMAGE = DockerImageName.parse("alpine:3.14");

    // withOptions {
    // Set up a plain OS container and customize environment,
    //   command and exposed ports. This just listens on port 80
    //   and always returns '42'
    @ClassRule
    public static GenericContainer<?> alpine =
        new GenericContainer<>(ALPINE_IMAGE)
                .withExposedPorts(80)
                .withEnv("MAGIC_NUMBER", "42")
                .withCommand("/bin/sh", "-c",
                "while true; do echo \"$MAGIC_NUMBER\" | nc -l -p 80; done");
    // }

    @Test
    public void testStartup() {
        assertTrue(redis.isRunning()); // good enough to check that the container started listening
        assertTrue(alpine.isRunning()); // good enough to check that the container started listening
    }
}
