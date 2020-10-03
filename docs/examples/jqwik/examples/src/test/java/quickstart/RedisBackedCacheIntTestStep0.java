package quickstart;


import net.jqwik.api.Disabled;
import net.jqwik.api.Example;
import net.jqwik.api.lifecycle.BeforeProperty;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("This test class is deliberately invalid, as it relies on a non-existent local Redis")
public class RedisBackedCacheIntTestStep0 {
    private RedisBackedCache underTest;

    @BeforeProperty
    public void setUp() {
        // Assume that we have Redis running locally?
        underTest = new RedisBackedCache("localhost", 6379);
    }

    @Example
    public void testSimplePutAndGet() {
        underTest.put("test", "example");

        String retrieved = underTest.get("test");
        assertThat("example").isEqualTo(retrieved);
    }
}
