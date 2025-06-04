package generic;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class CommandsTest {

    // startupCommand {
    @Container
    public GenericContainer redisWithCustomPort = new GenericContainer(DockerImageName.parse("redis:6-alpine"))
        .withCommand("redis-server --port 7777")
        // }
        .withExposedPorts(7777);

    @Test
    public void testStartupCommandOverrideApplied() {
        assertThat(redisWithCustomPort.isRunning()).isTrue(); // good enough to check that the container started listening
    }
}
