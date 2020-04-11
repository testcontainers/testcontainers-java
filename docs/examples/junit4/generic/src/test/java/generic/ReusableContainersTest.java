package generic;

import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import static org.junit.Assert.assertTrue;

public class ReusableContainersTest {

    private static GenericContainer<?> nginx;

    @BeforeClass
    public static void setUp() {
        nginx = new GenericContainer<>("nginx:1.17.9")
            .withExposedPorts(80)
            .withReuse(true);
    }

    @Test
    public void testContainersAllStarted() {
        assertTrue(nginx.isRunning());
    }
}
