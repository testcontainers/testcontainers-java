package generic;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

// class {
@Testcontainers
public class RedisContainerDependencyTest {

    @Container
    // redisserver {
    public GenericContainer redis = new GenericContainer<>("redis:5.0.3-alpine")
            .withExposedPorts(6379);
    // }

    @Container
    // rediscli {
    public GenericContainer redisCli = new GenericContainer<>("redis:5.0.3-alpine")
            .withExposedPorts(6380)
            .withCommand(String.format("redis-cli -h %s -p %d", redis.getContainerIpAddress(), redis.getFirstMappedPort()))
            .dependsOn(redis);
    // }

    @Test
    public void testSimplePutAndGet() {
        // Use containers in our test
    }
}
// }
