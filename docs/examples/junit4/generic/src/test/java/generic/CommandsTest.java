package generic;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandsTest {

    @Rule
    // startupCommand {
    public GenericContainer redisWithCustomPort = new GenericContainer(DockerImageName.parse("redis:5.0"))
        .withCommand("redis-server --port 7777")
        // }
        .withExposedPorts(7777);

    @Test
    public void testStartupCommandOverrideApplied() {
        assertThat(redisWithCustomPort.isRunning()).isTrue(); // good enough to check that the container started listening
    }
}
