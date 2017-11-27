package org.testcontainers.containers;

import lombok.Value;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Random;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.runner.Description.createTestDescription;

@RunWith(Parameterized.class)
@Value
public class DefaultRecordingFileFactoryTest {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("YYYYMMdd-HHmmss");

    private final DefaultRecordingFileFactory factory = new DefaultRecordingFileFactory();
    private final String methodName;
    private final String prefix;
    private final boolean success;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Random random = new Random();
        Collection<Object[]> args = new ArrayList<>();
        args.add(new Object[]{format("testMethodName%d", random.nextInt()), "FAILED", FALSE});
        args.add(new Object[]{format("testMethodName%d", random.nextInt()), "PASSED", TRUE});
        return args;
    }

    @Test
    public void recordingFileThatShouldDescribeTheTestResultAtThePresentTime() throws Exception {
        File vncRecordingDirectory = Files.createTempDirectory("recording").toFile();
        Description description = createTestDescription(getClass().getCanonicalName(), methodName, Test.class);

        File recordingFile = factory.recordingFileForTest(vncRecordingDirectory, description, success);

        File expectedFile = new File(vncRecordingDirectory, format("%s-%s-%s-%s.flv",
                prefix,
                getClass().getSimpleName(),
                methodName,
                DATE_FORMAT.format(new Date()))
        );
        assertEquals(expectedFile, recordingFile);
    }
}