package org.testcontainers.containers;

import lombok.Value;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.runner.Description.createTestDescription;

@RunWith(Parameterized.class)
@Value
public class DefaultRecordingFileFactoryTest {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("YYYYMMdd-HHmmss");

    private final DefaultRecordingFileFactory factory = new DefaultRecordingFileFactory();
    private final String methodName;
    private final String prefix;
    private final boolean success;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> args = new ArrayList<>();
        args.add(new Object[]{"testMethod1", "FAILED", FALSE});
        args.add(new Object[]{"testMethod2", "PASSED", TRUE});
        return args;
    }

    @Test
    public void recordingFileThatShouldDescribeTheTestResultAtThePresentTime() throws Exception {
        File vncRecordingDirectory = Files.createTempDirectory("recording").toFile();
        Description description = createTestDescription(getClass().getCanonicalName(), methodName, Test.class);

        File recordingFile = factory.recordingFileForTest(vncRecordingDirectory, description, success);

        String expectedFilePrefix = format("%s-%s-%s", prefix, getClass().getSimpleName(), methodName);

        List<File> expectedPossibleFileNames = Arrays.asList(
                new File(vncRecordingDirectory, format("%s-%s.flv", expectedFilePrefix, now().format(DATETIME_FORMATTER))),
                new File(vncRecordingDirectory, format("%s-%s.flv", expectedFilePrefix, now().minusSeconds(1L).format(DATETIME_FORMATTER)))
        );

        assertThat(expectedPossibleFileNames, hasItem(recordingFile));
    }
}
