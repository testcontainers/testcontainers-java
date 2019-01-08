package com.example;

import static org.rnorth.visibleassertions.VisibleAssertions.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

public class DemoControllerTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    public void simpleTest() {
        String fooResource = "/foo";

        info("putting 'bar' to " + fooResource);
        restTemplate.put(fooResource, "bar");

        assertEquals("value is set", "bar", restTemplate.getForObject(fooResource, String.class));
    }

}
