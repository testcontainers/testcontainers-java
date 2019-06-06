package quickstart

import spock.lang.Ignore
import spock.lang.Specification

@Ignore("This test class is deliberately invalid, as it relies on a non-existent local Redis")
class RedisBackedCacheIntTestStep0 extends Specification {
    private RedisBackedCache underTest

    void setup() {
        // Assume that we have Redis running locally?
        underTest = new RedisBackedCache("localhost", 6379)
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
