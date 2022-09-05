package generic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit4.Container;
import org.testcontainers.junit4.TestContainersRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(TestContainersRunner.class)
public class DependsOnTest {

    @Container
    // dependsOn {
    public GenericContainer<?> redis = new GenericContainer<>("redis:3.0.2").withExposedPorts(6379);

    @Container
    public GenericContainer<?> nginx = new GenericContainer<>("nginx:1.9.4").dependsOn(redis).withExposedPorts(80);

    // }

    @Test
    public void testContainersAllStarted() {
        assertThat(redis.isRunning()).isTrue();
        assertThat(nginx.isRunning()).isTrue();
    }
}
