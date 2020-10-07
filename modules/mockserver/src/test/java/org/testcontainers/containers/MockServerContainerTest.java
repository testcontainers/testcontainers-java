package org.testcontainers.containers;

import lombok.Cleanup;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThat;

public class MockServerContainerTest {

    public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName.parse("jamesdbloom/mockserver:mockserver-5.5.4");

    // creatingProxy {
    @Rule
    public MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE);
    // }

    private static String responseFromMockserver(MockServerContainer mockServer, String path) throws IOException {
        URLConnection urlConnection = new URL(mockServer.getEndpoint() + path).openConnection();
        @Cleanup BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        return reader.readLine();
    }

    @Test
    public void shouldReturnExpectation() throws Exception {
        // testSimpleExpectation {
        new MockServerClient(mockServer.getHost(), mockServer.getServerPort())
            .when(request()
                .withPath("/person")
                .withQueryStringParameter("name", "peter"))
            .respond(response()
                .withBody("Peter the person!"));

        // ...a GET request to '/person?name=peter' returns "Peter the person!"
        // }

        assertThat("Expectation returns expected response body",
            responseFromMockserver(mockServer, "/person?name=peter"),
            containsString("Peter the person")
        );
    }

    @Test
    public void shouldCallActualMockserverVersion() throws Exception {
        String actualVersion = MockServerClient.class.getPackage().getImplementationVersion();
        try (MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE.withTag("mockserver-" + actualVersion))) {
            mockServer.start();

            String expectedBody = "Hello World!";

            new MockServerClient(mockServer.getHost(), mockServer.getServerPort())
                .when(request().withPath("/hello"))
                .respond(response().withBody(expectedBody));

            assertThat("MockServer returns correct result",
                responseFromMockserver(mockServer, "/hello"),
                equalTo(expectedBody)
            );
        }
    }
}
