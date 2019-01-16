import com.example.DemoApplication;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.lifecycle.TestDescription;

import java.io.File;
import java.util.List;

import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL;

/**
 * Simple example of plain Selenium usage.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DemoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class SeleniumContainerTest {

    @LocalServerPort
    private int port;

    @Rule
    public BrowserWebDriverContainer chrome = new BrowserWebDriverContainer() {
        @Override
        public void beforeTest(TestDescription description) {
            Testcontainers.exposeHostPorts(port);
        }
    }
                                                    .withCapabilities(new ChromeOptions())
                                                    .withRecordingMode(RECORD_ALL, new File("target"));

    @Test
    public void simplePlainSeleniumTest() {
        RemoteWebDriver driver = chrome.getWebDriver();

        driver.get("http://host.testcontainers.internal:" + port + "/foo");
        List<WebElement> hElement = driver.findElementsByTagName("h");

        assertTrue("The h element is found", hElement != null && hElement.size() > 0);
    }
}
