package org.testcontainers.junit;


import com.google.common.io.PatternFilenameFilter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.DefaultRecordingFileFactory;
import org.testcontainers.lifecycle.TestDescription;

import java.util.Optional;
import static org.junit.Assert.assertTrue;
import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL;

@RunWith(Enclosed.class)
public class ChromeRecordingWebDriverContainerTest extends BaseWebDriverContainerTest {

    public static class ChromeThatRecordsAllTests {

        @Rule
        public TemporaryFolder vncRecordingDirectory = new TemporaryFolder();

        @Test
        public void recordingTestThatShouldBeRecordedAndRetained() {
            try (
                BrowserWebDriverContainer chrome = new BrowserWebDriverContainer()
                    .withCapabilities(new ChromeOptions())
                    .withRecordingMode(RECORD_ALL, vncRecordingDirectory.getRoot())
                    .withRecordingFileFactory(new DefaultRecordingFileFactory())
            ) {
                chrome.start();

                doSimpleExplore(chrome);
                chrome.afterTest(new TestDescription() {
                    @Override
                    public String getTestId() {
                        return getFilesystemFriendlyName();
                    }

                    @Override
                    public String getFilesystemFriendlyName() {
                        return "ChromeThatRecordsAllTests-recordingTestThatShouldBeRecordedAndRetained";
                    }
                }, Optional.empty());

                String[] files = vncRecordingDirectory.getRoot().list(new PatternFilenameFilter("PASSED-.*\\.flv"));
                assertTrue("Recorded file not found", files.length == 1);
            }
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
