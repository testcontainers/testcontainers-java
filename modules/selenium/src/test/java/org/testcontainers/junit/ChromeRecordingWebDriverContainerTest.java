package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.DefaultRecordingFileFactory;
import org.testcontainers.lifecycle.TestDescription;

import java.io.File;
import java.util.Optional;

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
        public void recordingTestThatShouldBeRecordedButNotPersisted() {
            doSimpleExplore(chrome);
        }

        @Test
        public void recordingTestThatShouldBeRecordedAndRetained() {
            doSimpleExplore(chrome);
            chrome.afterTest(new TestDescription() {
                @Override
                public String getTestId() {
                    return getFilesystemFriendlyName();
                }

                @Override
                public String getFilesystemFriendlyName() {
                    return "ChromeThatRecordsFailingTests-recordingTestThatShouldBeRecordedAndRetained";
                }
            }, Optional.of(new RuntimeException("Force writing of video file.")));
        }
    }
}
