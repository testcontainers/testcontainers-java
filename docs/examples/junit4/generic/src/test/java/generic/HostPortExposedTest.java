package generic;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

@org.testcontainers.junit.jupiter.Testcontainers
public class HostPortExposedTest {

    private static HttpServer server;

    private static int localServerPort;

    @BeforeAll
    public static void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(
            "/",
            exchange -> {
                byte[] content = "Hello World!".getBytes();
                exchange.sendResponseHeaders(200, content.length);
                try (OutputStream responseBody = exchange.getResponseBody()) {
                    responseBody.write(content);
                    responseBody.flush();
                }
            }
        );

        server.start();
        localServerPort = server.getAddress().getPort();

        // exposePort {
        Testcontainers.exposeHostPorts(localServerPort);
        // }
    }

    @AfterAll
    public static void tearDown() throws Exception {
        server.stop(0);
    }

    @Container
    public BrowserWebDriverContainer<?> browser = new BrowserWebDriverContainer<>()
        .withCapabilities(new ChromeOptions());

    @Test
    public void testContainerRunningAgainstExposedHostPort() {
        // useHostExposedPort {
        final String rootUrl = String.format("http://host.testcontainers.internal:%d/", localServerPort);

        final RemoteWebDriver webDriver = new RemoteWebDriver(this.browser.getSeleniumAddress(), new ChromeOptions());
        webDriver.get(rootUrl);
        // }

        final String pageSource = webDriver.getPageSource();
        assertThat(pageSource).contains("Hello World!");
    }
}
