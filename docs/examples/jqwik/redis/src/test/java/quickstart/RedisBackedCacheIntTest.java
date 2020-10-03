package quickstart;


import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jqwik.Container;
import org.testcontainers.junit.jqwik.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class RedisBackedCacheIntTest {

    private RedisBackedCache underTest;

    @Container
    public GenericContainer redis = new GenericContainer(DockerImageName.parse("redis:5.0.3-alpine"))
                                            .withExposedPorts(6379);

    @BeforeProperty
    public void setUp() {
        String address = redis.getHost();
        Integer port = redis.getFirstMappedPort();

        // Now we have an address and port for Redis, no matter where it is running
        underTest = new RedisBackedCache(address, port);
    }

    @Example
    public void retrieve_from_redis() {
        underTest.put("test", "example");

        String retrieved = underTest.get("test");
        assertThat("example").isEqualTo(retrieved);
    }

    @Property
    public void what_has_been_put_in_redis_must_be_retrievable(@ForAll String key, @ForAll String value){
        underTest.put(key, value);
        String retrieved = underTest.get(key);
        assertThat(retrieved).isEqualTo(value);
    }
}
