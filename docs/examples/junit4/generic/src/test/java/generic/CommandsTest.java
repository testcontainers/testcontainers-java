package generic;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit4.TestcontainersRule;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandsTest {

    @Rule
    // startupCommand {
    public TestcontainersRule<GenericContainer<?>> redisWithCustomPort = new TestcontainersRule<>(
        new GenericContainer(DockerImageName.parse("redis:6-alpine"))
            .withCommand("redis-server --port 7777")
            // }
            .withExposedPorts(7777)
    );

    @Test
    public void testStartupCommandOverrideApplied() {
        assertThat(redisWithCustomPort.get().isRunning()).isTrue(); // good enough to check that the container started listening
    }
}
