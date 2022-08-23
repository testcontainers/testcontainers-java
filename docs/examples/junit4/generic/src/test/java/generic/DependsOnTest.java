package generic;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

public class DependsOnTest {

    @Rule
    // dependsOn {
    public GenericContainer<?> redis = new GenericContainer<>("redis:3.0.2").withExposedPorts(6379);

    @Rule
    public GenericContainer<?> nginx = new GenericContainer<>("nginx:1.9.4").dependsOn(redis).withExposedPorts(80);

    // }

    @Test
    public void testContainersAllStarted() {
        assertThat(redis.isRunning()).isTrue();
        assertThat(nginx.isRunning()).isTrue();
    }
}
