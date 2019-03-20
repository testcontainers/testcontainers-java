package org.testcontainers.containers;

import lombok.Cleanup;
import org.junit.Test;
import org.mockserver.client.MockServerClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThat;

public class MockServerContainerTest {

    @Test
    public void shouldCallActualMockserverVersion() throws Exception {
        String actualVersion = MockServerClient.class.getPackage().getImplementationVersion();
        try (MockServerContainer mockServer = new MockServerContainer(actualVersion)) {
            mockServer.start();

            String expectedBody = "Hello World!";

            assertThat("MockServer returns correct result",
                responseFromMockserver(mockServer, expectedBody, "/hello"),
                containsString(expectedBody)
            );
        }
    }

    @Test
    public void shouldCallDefaultMockserverVersion() throws Exception {
        try (MockServerContainer mockServerDefault = new MockServerContainer()) {
            mockServerDefault.start();
            String expectedBody = "Hello Default World!";

            assertThat("MockServer returns correct result for default constructor",
                responseFromMockserver(mockServerDefault, expectedBody, "/hellodefault"),
                containsString(expectedBody)
            );
        }
    }

    private static String responseFromMockserver(MockServerContainer mockServer, String expectedBody, String path) throws IOException {
        new MockServerClient(mockServer.getContainerIpAddress(), mockServer.getServerPort())
            .when(request(path))
            .respond(response(expectedBody));

        URLConnection urlConnection = new URL(mockServer.getEndpoint() + path).openConnection();
        @Cleanup BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        return reader.readLine();
    }
}
