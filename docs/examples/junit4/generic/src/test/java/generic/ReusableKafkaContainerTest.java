package generic;

import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;

import static org.junit.Assert.assertTrue;

public class ReusableKafkaContainerTest {

    private static KafkaContainer kafka;

    @BeforeClass
    public static void setUp() {
        kafka = new KafkaContainer()
            .withNetwork(null)
            .withReuse(true);
        kafka.start();
    }

    @Test
    public void testKafkaContainer() {
        assertTrue(kafka.isRunning());
    }
}
