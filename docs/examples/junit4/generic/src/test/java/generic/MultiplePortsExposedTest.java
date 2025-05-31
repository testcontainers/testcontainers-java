package generic;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit4.TestcontainersRule;
import org.testcontainers.utility.DockerImageName;

public class MultiplePortsExposedTest {

    private static final Logger log = LoggerFactory.getLogger(MultiplePortsExposedTest.class);

    @Rule
    // rule {
    public TestcontainersRule<GenericContainer<?>> container = new TestcontainersRule<>(
        new GenericContainer<>(DockerImageName.parse("testcontainers/helloworld:1.1.0"))
            .withExposedPorts(8080, 8081)
            .withLogConsumer(new Slf4jLogConsumer(log))
    );

    // }

    @Test
    public void fetchPortsByNumber() {
        Integer firstMappedPort = container.get().getMappedPort(8080);
        Integer secondMappedPort = container.get().getMappedPort(8081);
    }

    @Test
    public void fetchFirstMappedPort() {
        Integer firstMappedPort = container.get().getFirstMappedPort();
    }

    @Test
    public void getHostOnly() {
        String ipAddress = container.get().getHost();
    }

    @Test
    public void getHostAndMappedPort() {
        String address = container.get().getHost() + ":" + container.get().getMappedPort(8080);
    }
}
