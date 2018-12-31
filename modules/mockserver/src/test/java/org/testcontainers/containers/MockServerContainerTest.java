package org.testcontainers.containers;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

@Slf4j
public class MockServerContainerTest {

    @ClassRule
    public static MockServerContainer mockServer = new MockServerContainer(MockServerClient.class.getPackage().getImplementationVersion())
        .withLogConsumer(new Slf4jLogConsumer(log));

    @Test
    public void testBasicScenario() throws Exception {
        new MockServerClient(mockServer.getContainerIpAddress(), mockServer.getServerPort())
            .when(request("/hello"))
            .respond(response("Hello World!"));

        URLConnection urlConnection = new URL(mockServer.getEndpoint() + "/hello").openConnection();
        @Cleanup BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String line = reader.readLine();
        System.out.println(line);

        assertTrue("MockServer returns correct result", line.contains("Hello World!"));
    }
}
