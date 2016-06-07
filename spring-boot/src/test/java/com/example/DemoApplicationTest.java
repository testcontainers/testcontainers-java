package com.example;

import org.junit.Test;
import org.springframework.web.client.RestTemplate;


import static org.rnorth.visibleassertions.VisibleAssertions.*;

public class DemoApplicationTest extends AbstractIntegrationTest {

    RestTemplate restTemplate = new RestTemplate();

    @Test
    public void simpleTest() {
        String fooResource = "http://localhost:" + port + "/foo";

        info("putting 'bar' to " + fooResource);
        restTemplate.put(fooResource, "bar");

        assertEquals("value is set", "bar", restTemplate.getForObject(fooResource, String.class));
    }

}
