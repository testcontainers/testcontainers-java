package org.testcontainers.containers;

import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.configuration.Configuration;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.socket.tls.KeyStoreFactory;
import org.testcontainers.utility.DockerImageName;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class MockServerContainerTest {

    public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
        .parse("mockserver/mockserver")
        .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());

    @Test
    public void shouldCallActualMockserverVersion() throws Exception {
        try (MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE)) {
            mockServer.start();

            String expectedBody = "Hello World!";

            try (MockServerClient client = new MockServerClient(mockServer.getHost(), mockServer.getServerPort())) {
                assertThat(client.hasStarted()).as("Mockserver running").isTrue();

                client.when(request().withPath("/hello")).respond(response().withBody(expectedBody));

                assertThat(given().when().get(mockServer.getEndpoint() + "/hello").then().extract().body().asString())
                    .as("MockServer returns correct result")
                    .isEqualTo(expectedBody);
            }
        }
    }

    @Test
    public void shouldCallMockserverUsingTlsProtocol() throws Exception {
        try (MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE)) {
            mockServer.start();

            String expectedBody = "Hello World!";

            try (
                MockServerClient client = new MockServerClient(mockServer.getHost(), mockServer.getServerPort())
                    .withSecure(true)
            ) {
                assertThat(client.hasStarted()).as("Mockserver running").isTrue();

                client.when(request().withPath("/hello")).respond(response().withBody(expectedBody));

                assertThat(secureResponseFromMockserver(mockServer))
                    .as("MockServer returns correct result")
                    .isEqualTo(expectedBody);
            }
        }
    }

    @Test
    public void shouldCallMockserverUsingMutualTlsProtocol() throws Exception {
        try (
            MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE)
                .withEnv("MOCKSERVER_TLS_MUTUAL_AUTHENTICATION_REQUIRED", "true")
        ) {
            mockServer.start();

            String expectedBody = "Hello World!";

            try (
                MockServerClient client = new MockServerClient(mockServer.getHost(), mockServer.getServerPort())
                    .withSecure(true)
            ) {
                assertThat(client.hasStarted()).as("Mockserver running").isTrue();

                client.when(request().withPath("/hello")).respond(response().withBody(expectedBody));

                assertThat(secureResponseFromMockserver(mockServer))
                    .as("MockServer returns correct result")
                    .isEqualTo(expectedBody);
            }
        }
    }

    @Test
    public void newVersionStartsWithDefaultWaitStrategy() {
        try (MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE)) {
            mockServer.start();
        }
    }

    private static String secureResponseFromMockserver(MockServerContainer mockServer) {
        return given()
            .config(
                RestAssuredConfig
                    .config()
                    .sslConfig(
                        SSLConfig
                            .sslConfig()
                            .sslSocketFactory(
                                new SSLSocketFactory(
                                    new KeyStoreFactory(Configuration.configuration(), new MockServerLogger())
                                        .sslContext()
                                )
                            )
                    )
            )
            .baseUri(mockServer.getSecureEndpoint())
            .get("/hello")
            .body()
            .asString();
    }
}
