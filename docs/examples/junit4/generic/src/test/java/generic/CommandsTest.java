package generic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit4.Container;
import org.testcontainers.junit4.TestContainersRunner;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(TestContainersRunner.class)
public class CommandsTest {

    @Container
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
