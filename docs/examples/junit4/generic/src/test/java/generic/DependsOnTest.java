package generic;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class DependsOnTest {

    // dependsOn {
    @Container
    public GenericContainer<?> redis = new GenericContainer<>("redis:6-alpine").withExposedPorts(6379);

    @Container
    public GenericContainer<?> nginx = new GenericContainer<>("nginx:1.27.0-alpine3.19-slim")
        .dependsOn(redis)
        .withExposedPorts(80);

    // }

    @Test
    public void testContainersAllStarted() {
        assertThat(redis.isRunning()).isTrue();
        assertThat(nginx.isRunning()).isTrue();
    }
}
