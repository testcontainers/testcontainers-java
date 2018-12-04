package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.DefaultRecordingFileFactory;

import java.io.File;

import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL;

@RunWith(Enclosed.class)
public class ChromeRecordingWebDriverContainerTest extends BaseWebDriverContainerTest {

    public static class ChromeThatRecordsAllTests {

        @Rule
        public BrowserWebDriverContainer chrome = new BrowserWebDriverContainer()
                .withCapabilities(new ChromeOptions())
                .withRecordingMode(RECORD_ALL, new File("./build/"))
                .withRecordingFileFactory(new DefaultRecordingFileFactory());

        @Test
        public void recordingTestThatShouldBeRecordedAndRetained() {
            doSimpleExplore(chrome);
        }
    }

    public static class ChromeThatRecordsFailingTests {

        @Rule
        public BrowserWebDriverContainer chrome = new BrowserWebDriverContainer()
                .withCapabilities(new ChromeOptions());

        @Test
        public void recordingTestThatShouldBeRecordedButDeleted() {
            doSimpleExplore(chrome);
        }
    }
}
