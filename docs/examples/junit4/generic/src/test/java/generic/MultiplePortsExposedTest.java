package generic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit4.Container;
import org.testcontainers.junit4.TestContainersRunner;
import org.testcontainers.utility.DockerImageName;

@RunWith(TestContainersRunner.class)
public class MultiplePortsExposedTest {

    private static final Logger log = LoggerFactory.getLogger(MultiplePortsExposedTest.class);

    @Container
    // rule {
    public GenericContainer<?> container = new GenericContainer<>(
        DockerImageName.parse("testcontainers/helloworld:1.1.0")
    )
        .withExposedPorts(8080, 8081)
        .withLogConsumer(new Slf4jLogConsumer(log));

    // }

    @Test
    public void fetchPortsByNumber() {
        Integer firstMappedPort = container.getMappedPort(8080);
        Integer secondMappedPort = container.getMappedPort(8081);
    }

    @Test
    public void fetchFirstMappedPort() {
        Integer firstMappedPort = container.getFirstMappedPort();
    }

    @Test
    public void getHostOnly() {
        String ipAddress = container.getHost();
    }

    @Test
    public void getHostAndMappedPort() {
        String address = container.getHost() + ":" + container.getMappedPort(8080);
    }
}
