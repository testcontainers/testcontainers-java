package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testcontainers.containers.BrowserWebDriverContainer;

import java.io.File;

import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL;

/**
 *
 */
public class ChromeRecordingWebDriverContainerTest extends BaseWebDriverContainerTest {

    @Rule
    public BrowserWebDriverContainer chromeThatRecordsAllTests = new BrowserWebDriverContainer()
            .withDesiredCapabilities(DesiredCapabilities.chrome())
            .withRecordingMode(RECORD_ALL, new File("./target/"));

    @Rule
    public BrowserWebDriverContainer chromeThatRecordsFailingTests = new BrowserWebDriverContainer()
            .withDesiredCapabilities(DesiredCapabilities.chrome());


    @Test
    public void recordingTestThatShouldBeRecordedButDeleted() {
        doSimpleExplore(chromeThatRecordsFailingTests);
    }

    @Test
    public void recordingTestThatShouldBeRecordedAndRetained() {
        doSimpleExplore(chromeThatRecordsAllTests);
    }
}
