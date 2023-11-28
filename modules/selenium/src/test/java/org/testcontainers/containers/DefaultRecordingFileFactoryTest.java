package org.testcontainers.containers;

import lombok.Value;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        args.add(new Object[] { "testMethod1", "FAILED", Boolean.FALSE });
        args.add(new Object[] { "testMethod2", "PASSED", Boolean.TRUE });
        return args;
    }

    @Test
    public void recordingFileThatShouldDescribeTheTestResultAtThePresentTime() throws Exception {
        File vncRecordingDirectory = Files.createTempDirectory("recording").toFile();
        Description description = Description.createTestDescription(
            getClass().getCanonicalName(),
            methodName,
            Test.class
        );

        File recordingFile = factory.recordingFileForTest(vncRecordingDirectory, description, success);

        String expectedFilePrefix = String.format("%s-%s-%s", prefix, getClass().getSimpleName(), methodName);

        List<File> expectedPossibleFileNames = Arrays.asList(
            new File(
                vncRecordingDirectory,
                String.format("%s-%s.flv", expectedFilePrefix, LocalDateTime.now().format(DATETIME_FORMATTER))
            ),
            new File(
                vncRecordingDirectory,
                String.format(
                    "%s-%s.flv",
                    expectedFilePrefix,
                    LocalDateTime.now().minusSeconds(1L).format(DATETIME_FORMATTER)
                )
            )
        );

        assertThat(expectedPossibleFileNames).contains(recordingFile);
    }
}
