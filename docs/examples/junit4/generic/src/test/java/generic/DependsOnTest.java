package generic;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit4.TestcontainersRule;

import static org.assertj.core.api.Assertions.assertThat;

public class DependsOnTest {

    @Rule
    // dependsOn {
    public TestcontainersRule<GenericContainer<?>> redis = new TestcontainersRule<>(
        new GenericContainer<>("redis:6-alpine").withExposedPorts(6379)
    );

    @Rule
    public TestcontainersRule<GenericContainer<?>> nginx = new TestcontainersRule<>(
        new GenericContainer<>("nginx:1.27.0-alpine3.19-slim").dependsOn(redis.get()).withExposedPorts(80)
    );

    // }

    @Test
    public void testContainersAllStarted() {
        assertThat(redis.get().isRunning()).isTrue();
        assertThat(nginx.get().isRunning()).isTrue();
    }
}
