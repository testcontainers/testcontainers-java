package quickstart;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit4.Container;
import org.testcontainers.junit4.TestContainersRunner;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

// class {
@RunWith(TestContainersRunner.class)
public class RedisBackedCacheIntTest {

    private RedisBackedCache underTest;

    // rule {
    @Container
    public GenericContainer redis = new GenericContainer(DockerImageName.parse("redis:5.0.3-alpine"))
        .withExposedPorts(6379);

    // }

    @Before
    public void setUp() {
        String address = redis.getHost();
        Integer port = redis.getFirstMappedPort();

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
// }
