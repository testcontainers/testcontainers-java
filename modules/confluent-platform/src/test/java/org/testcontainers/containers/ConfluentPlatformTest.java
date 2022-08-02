package org.testcontainers.containers;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class ConfluentPlatformTest {

    @Before
    public void setup() {

    }


    @Test
    public void testCreatePlatform() throws IOException {
//        String currentPath = new java.io.File(".").getCanonicalPath();
//        System.out.println("Current dir:" + currentPath);
        String payload = String.join("",Files.readAllLines(Paths.get("src/test/resources/stock_gen.json")));
        ConfluentPlatform confluentPlatform = new ConfluentPlatform();
        confluentPlatform.createTopic("test");

        confluentPlatform.createConnect(payload);

        confluentPlatform.createTopic("test_v1");
        confluentPlatform.isTopicExists("test_v1");
        confluentPlatform.deleteTopic("test_v1");
        confluentPlatform.isTopicExists("test_v1");

        while (true) {
            try {
                Thread.sleep(1000 * 10);
            } catch (InterruptedException e) {
                System.out.println("ssss");
            }
        }
    }
}
