package quickstart;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit4.TestcontainersRule;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

public class RedisBackedCacheIntTest {

    private RedisBackedCache underTest;

    // rule {
    @Rule
    public TestcontainersRule<GenericContainer<?>> redis = new TestcontainersRule<>(
        new GenericContainer<>(DockerImageName.parse("redis:6-alpine")).withExposedPorts(6379)
    );

    // }

    @Before
    public void setUp() {
        String address = redis.get().getHost();
        Integer port = redis.get().getFirstMappedPort();

        // Now we have an address and port for Redis, no matter where it is running
        underTest = new RedisBackedCache(address, port);
    }

    @Test
    public void testSimplePutAndGet() {
        underTest.put("test", "example");

        String retrieved = underTest.get("test");
        assertThat(retrieved).isEqualTo("example");
    }
}
