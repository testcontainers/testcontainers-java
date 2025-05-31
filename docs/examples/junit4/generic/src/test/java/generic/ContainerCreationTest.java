package generic;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit4.TestcontainersRule;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

public class ContainerCreationTest {

    // spotless:off
    // simple {
    public static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:6-alpine");

    @ClassRule
    public static TestcontainersRule<GenericContainer<?>> redis = new TestcontainersRule<>(
        new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379));

    // }
    // spotless:on

    public static final DockerImageName ALPINE_IMAGE = DockerImageName.parse("alpine:3.17");

    // spotless:off
    // withOptions {
    // Set up a plain OS container and customize environment,
    //   command and exposed ports. This just listens on port 80
    //   and always returns '42'
    @ClassRule
    public static TestcontainersRule<GenericContainer<?>> alpine = new TestcontainersRule<>(
        new GenericContainer<>(ALPINE_IMAGE)
            .withExposedPorts(80)
            .withEnv("MAGIC_NUMBER", "42")
            .withCommand("/bin/sh", "-c",
                "while true; do echo \"$MAGIC_NUMBER\" | nc -l -p 80; done"));

    // }
    // spotless:on

    @Test
    public void testStartup() {
        assertThat(redis.get().isRunning()).isTrue(); // good enough to check that the container started listening
        assertThat(alpine.get().isRunning()).isTrue(); // good enough to check that the container started listening
    }
}
