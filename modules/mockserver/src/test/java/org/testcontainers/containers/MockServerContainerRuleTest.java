package org.testcontainers.containers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.testcontainers.utility.DockerImageName;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class MockServerContainerRuleTest {

    // creatingProxy {
    private static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
        .parse("mockserver/mockserver")
        .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());

    private static MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE);

    // }

    @BeforeAll
    static void setup() {
        mockServer.start();
    }

    @AfterAll
    static void tearDown() {
        mockServer.stop();
    }

    @Test
    void shouldReturnExpectation() {
        // testSimpleExpectation {
        try (
            MockServerClient mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort())
        ) {
            mockServerClient
                .when(request().withPath("/person").withQueryStringParameter("name", "peter"))
                .respond(response().withBody("Peter the person!"));

            // ...a GET request to '/person?name=peter' returns "Peter the person!"

            assertThat(
                given().baseUri(mockServer.getEndpoint()).get("/person?name=peter").then().extract().body().asString()
            )
                .as("Expectation returns expected response body")
                .contains("Peter the person");
        }
        // }
    }
}
