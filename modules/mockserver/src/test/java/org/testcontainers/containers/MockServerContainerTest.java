package org.testcontainers.containers;

import lombok.Cleanup;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

public class MockServerContainerTest {

    @ClassRule
    public static MockServerContainer mockServer = new MockServerContainer();

    @Test
    public void testBasicScenario() throws Exception {
        mockServer.getClient()
            .when(request("/hello"))
            .respond(response("Hello World!"));

        URLConnection urlConnection = new URL(mockServer.getEndpoint() + "/hello").openConnection();
        @Cleanup BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String line = reader.readLine();
        System.out.println(line);

        assertTrue("MockServer returns correct result", line.contains("Hello World!"));
    }
}
