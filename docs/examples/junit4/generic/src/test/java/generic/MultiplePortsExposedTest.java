package generic;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import static org.slf4j.LoggerFactory.getLogger;

public class MultiplePortsExposedTest {
    private static final Logger log = getLogger(MultiplePortsExposedTest.class);


    @Rule
    // rule {
    public GenericContainer container = new GenericContainer(DockerImageName.parse("orientdb:3.0.13"))
        .withExposedPorts(2424, 2480)
        .withLogConsumer(new Slf4jLogConsumer(log));
    // }

    @Test
    public void fetchPortsByNumber() {
        Integer firstMappedPort = container.getMappedPort(2424);
        Integer secondMappedPort = container.getMappedPort(2480);
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
        String address =
            container.getHost() + ":" + container.getMappedPort(2424);
    }
}
