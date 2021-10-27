package org.testcontainers.containers;

import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThat;

public class MockServerContainerRuleTest {

    public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName.parse("jamesdbloom/mockserver:mockserver-5.5.4");

    // creatingProxy {
    @Rule
    public MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE);
    // }

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
            SimpleHttpClient.responseFromMockserver(mockServer, "/person?name=peter"),
            containsString("Peter the person")
        );
    }
}
