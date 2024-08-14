package generic;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

public class DependsOnTest {

    @Rule
    // dependsOn {
    public GenericContainer<?> redis = new GenericContainer<>("redis:6-alpine").withExposedPorts(6379);

    @Rule
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
