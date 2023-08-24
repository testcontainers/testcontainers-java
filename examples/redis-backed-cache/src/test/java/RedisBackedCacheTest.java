import com.mycompany.cache.Cache;
import com.mycompany.cache.RedisBackedCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Redis-backed cache implementation.
 */
@Testcontainers
class RedisBackedCacheTest {

    @Container
    public GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:3.0.6"))
        .withExposedPorts(6379);

    private Cache cache;

    @BeforeEach
    void setUp() throws Exception {
        Jedis jedis = new Jedis(redis.getHost(), redis.getMappedPort(6379));

        cache = new RedisBackedCache(jedis, "test");
    }

    @Test
    void testFindingAnInsertedValue() {
        cache.put("foo", "FOO");
        Optional<String> foundObject = cache.get("foo", String.class);

        assertThat(foundObject.isPresent()).as("When an object in the cache is retrieved, it can be found").isTrue();
        assertThat(foundObject.get())
            .as("When we put a String in to the cache and retrieve it, the value is the same")
            .isEqualTo("FOO");
    }

    @Test
    void testNotFindingAValueThatWasNotInserted() {
        Optional<String> foundObject = cache.get("bar", String.class);

        assertThat(foundObject.isPresent())
            .as("When an object that's not in the cache is retrieved, nothing is found")
            .isFalse();
    }
}
