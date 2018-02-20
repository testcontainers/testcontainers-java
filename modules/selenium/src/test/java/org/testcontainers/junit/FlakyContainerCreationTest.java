package org.testcontainers.junit;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;

import java.io.File;

/**
 * Created by rnorth on 30/01/2016.
 */
public class FlakyContainerCreationTest {

    @Test @Ignore
    public void testCreationOfManyContainers() {
        for (int i = 0; i < 50; i++) {
            BrowserWebDriverContainer container = new BrowserWebDriverContainer()
                    .withDesiredCapabilities(DesiredCapabilities.chrome())
                    .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.RECORD_FAILING, new File("build"));

            container.start();
            RemoteWebDriver driver = container.getWebDriver();

            driver.get("http://www.google.com");

            container.stop();
        }
    }
}
