package generic;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class ContainerCreationTest {

    // spotless:off
    // simple {
    public static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:6-alpine");

    @Container
    public static GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE)
        .withExposedPorts(6379);

    // }
    // spotless:on

    public static final DockerImageName ALPINE_IMAGE = DockerImageName.parse("alpine:3.17");

    // spotless:off
    // withOptions {
    // Set up a plain OS container and customize environment,
    //   command and exposed ports. This just listens on port 80
    //   and always returns '42'
    @Container
    public static GenericContainer<?> alpine = new GenericContainer<>(ALPINE_IMAGE)
        .withExposedPorts(80)
        .withEnv("MAGIC_NUMBER", "42")
        .withCommand("/bin/sh", "-c",
            "while true; do echo \"$MAGIC_NUMBER\" | nc -l -p 80; done");

    // }
    // spotless:on

    @Test
    public void testStartup() {
        assertThat(redis.isRunning()).isTrue(); // good enough to check that the container started listening
        assertThat(alpine.isRunning()).isTrue(); // good enough to check that the container started listening
    }
}
