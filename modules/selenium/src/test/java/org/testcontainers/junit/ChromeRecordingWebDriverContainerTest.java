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

import java.io.File;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL;
import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_FAILING;

@RunWith(Enclosed.class)
public class ChromeRecordingWebDriverContainerTest extends BaseWebDriverContainerTest {

    public static class ChromeThatRecordsAllTests {

        @Rule
        public TemporaryFolder vncRecordingDirectory = new TemporaryFolder();

        @Test
        public void recordingTestThatShouldBeRecordedAndRetained() {
            File target = vncRecordingDirectory.getRoot();
            try (
                // recordAll {
                // To do this, simply add extra parameters to the rule constructor:
                BrowserWebDriverContainer chrome = new BrowserWebDriverContainer()
                    .withCapabilities(new ChromeOptions())
                    .withRecordingMode(RECORD_ALL, target)
                    // }
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
                assertEquals("Recorded file not found", 1, files.length);
            }
        }
    }

    public static class ChromeThatRecordsFailingTests {

        @Rule
        public TemporaryFolder vncRecordingDirectory = new TemporaryFolder();

        @Test
        public void recordingTestThatShouldBeRecordedButNotPersisted() {
            try (
                // withRecordingFileFactory {
                BrowserWebDriverContainer chrome = new BrowserWebDriverContainer()
                    // }
                    .withCapabilities(new ChromeOptions())
                    // withRecordingFileFactory {
                    .withRecordingFileFactory(new CustomRecordingFileFactory())
                // }
            ) {
                chrome.start();

                doSimpleExplore(chrome);
            }
        }

        @Test
        public void recordingTestThatShouldBeRecordedAndRetained() {
            File target = vncRecordingDirectory.getRoot();
            try (
                // recordFailing {
                // or if you only want videos for test failures:
                BrowserWebDriverContainer chrome = new BrowserWebDriverContainer()
                    .withCapabilities(new ChromeOptions())
                    .withRecordingMode(RECORD_FAILING, target)
                    // }
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
                        return "ChromeThatRecordsFailingTests-recordingTestThatShouldBeRecordedAndRetained";
                    }
                }, Optional.of(new RuntimeException("Force writing of video file.")));

                String[] files = vncRecordingDirectory.getRoot().list(new PatternFilenameFilter("FAILED-.*\\.flv"));
                assertEquals("Recorded file not found", 1, files.length);
            }

        }

        private static class CustomRecordingFileFactory extends DefaultRecordingFileFactory { }
    }
}
