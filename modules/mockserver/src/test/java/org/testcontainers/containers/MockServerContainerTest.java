package org.testcontainers.containers;

import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.FileNotFoundException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThat;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;

public class MockServerContainerTest {

    public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName.parse("mockserver/mockserver:mockserver-5.11.2");

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
                SimpleHttpClient.responseFromMockserver(mockServer, "/hello"),
                equalTo(expectedBody)
            );
        }
    }

    @Test
    public void oldVersionRequiresDefaultWaitStrategy() throws Exception {
        DockerImageName dockerImageName = DockerImageName.parse("jamesdbloom/mockserver").withTag("mockserver-5.5.4");
        try (MockServerContainer mockServer = new MockServerContainer(dockerImageName).waitingFor(Wait.defaultWaitStrategy())) {
            mockServer.start();

            assertThrows("expected not found response (as we can't use old client to set expectations properly)", FileNotFoundException.class,
                () -> SimpleHttpClient.responseFromMockserver(mockServer, "/hello")
            );
        }
    }
}
