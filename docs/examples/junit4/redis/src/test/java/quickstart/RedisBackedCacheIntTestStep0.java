package quickstart;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("This test class is deliberately invalid, as it relies on a non-existent local Redis")
public class RedisBackedCacheIntTestStep0 {

    private RedisBackedCache underTest;

    @Before
    public void createCache() {
        // Assume that we have Redis running locally?
        underTest = new RedisBackedCache("localhost", 6379);
    }

    @Test
    public void simplePutAndGet() {
        underTest.put("test", "example");

        String retrieved = underTest.get("test");
        assertThat(retrieved).isEqualTo("example");
    }
}
