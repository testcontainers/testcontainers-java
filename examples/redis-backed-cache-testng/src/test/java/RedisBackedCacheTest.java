import com.mycompany.cache.Cache;
import com.mycompany.cache.RedisBackedCache;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import redis.clients.jedis.Jedis;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Redis-backed cache implementation.
 */
public class RedisBackedCacheTest {

    private static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:6-alpine"))
        .withExposedPorts(6379);

    private Cache cache;

    @BeforeClass
    public static void startContainer() {
        redis.start();
    }

    @AfterClass
    public static void stopContainer() {
        redis.stop();
    }

    @BeforeMethod
    public void setUp() {
        try (Jedis jedis = new Jedis(redis.getHost(), redis.getMappedPort(6379))) {
            cache = new RedisBackedCache(jedis, "test");
        }
    }

    @Test
    public void testFindingAnInsertedValue() {
        cache.put("foo", "FOO");
        Optional<String> foundObject = cache.get("foo", String.class);

        assertThat(foundObject).as("When an object in the cache is retrieved, it can be found").isPresent();
        assertThat(foundObject)
            .as("When we put a String in to the cache and retrieve it, the value is the same")
            .contains("FOO");
    }

    @Test
    public void testNotFindingAValueThatWasNotInserted() {
        Optional<String> foundObject = cache.get("bar", String.class);

        assertThat(foundObject)
            .as("When an object that's not in the cache is retrieved, nothing is found")
            .isNotPresent();
    }
}
