package generic;

import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import static org.junit.Assert.assertTrue;

public class ReusableContainersTest {

    // reusable_containers {
    private static GenericContainer<?> nginx;

    @BeforeClass
    public static void setUp() {
        nginx = new GenericContainer<>("nginx:1.17.9")
            .withExposedPorts(80)
            .waitingFor(Wait.forHttp("/"))
            .withReuse(true);
        nginx.start();
    }
    // }

    @Test
    public void testContainersAllStarted() {
        assertTrue(nginx.isRunning());
    }
}
