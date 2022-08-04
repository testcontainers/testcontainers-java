package org.testcontainers.containers;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.assertEquals;


public class ConfluentPlatformTest {

    private static ConfluentPlatform confluentPlatform = null;
    @Before
    public void setup() {
        confluentPlatform = new ConfluentPlatform();
    }


    @Test
    public void testCreatePlatform()  {
        assertEquals("create topic", true, confluentPlatform.createTopic("test_v1"));
        assertEquals("check topic test_v1", true, confluentPlatform.isTopicExists("test_v1"));
        assertEquals("check topic none_v1", false, confluentPlatform.isTopicExists("none_v1"));
    }

    public void testCreateDataGen() throws IOException {
        String payload = String.join("",Files.readAllLines(Paths.get("src/test/resources/stock_gen.json")));
        confluentPlatform.createTopic("test");
        confluentPlatform.createConnect(payload);
        assertEquals("create topic", true, confluentPlatform.createTopic("test"));
    }
}
