package quickstart

import org.testcontainers.containers.GenericContainer
import spock.lang.Specification

// complete {
@org.testcontainers.spock.Testcontainers
class RedisBackedCacheIntTest extends Specification {

    private RedisBackedCache underTest

    // init {
    GenericContainer redis = new GenericContainer<>("redis:5.0.3-alpine")
        .withExposedPorts(6379)
    // }

    void setup() {
        String address = redis.host
        Integer port = redis.firstMappedPort

        // Now we have an address and port for Redis, no matter where it is running
        underTest = new RedisBackedCache(address, port)
    }

    void testSimplePutAndGet() {
        setup:
        underTest.put("test", "example")

        when:
        String retrieved = underTest.get("test")

        then:
        retrieved == "example"
    }
}
// }
