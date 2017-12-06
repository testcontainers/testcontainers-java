package org.testcontainers.containers;

import org.junit.runner.Description;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DefaultRecordingFileFactory implements RecordingFileFactory {

    private static final SimpleDateFormat filenameDateFormat = new SimpleDateFormat("YYYYMMdd-HHmmss");
    private static final String PASSED = "PASSED";
    private static final String FAILED = "FAILED";
    private static final String FILENAME_FORMAT = "%s-%s-%s-%s.flv";

    @Override
    public File recordingFileForTest(File vncRecordingDirectory, Description description, boolean succeeded) {
        final String prefix = succeeded ? PASSED : FAILED;
        final String fileName = String.format(FILENAME_FORMAT,
                prefix,
                description.getTestClass().getSimpleName(),
                description.getMethodName(),
                filenameDateFormat.format(new Date())
        );
        return new File(vncRecordingDirectory, fileName);
    }
}
