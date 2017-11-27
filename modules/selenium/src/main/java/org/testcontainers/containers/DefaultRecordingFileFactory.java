package org.testcontainers.containers;

import org.junit.runner.Description;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DefaultRecordingFileFactory implements RecordingFileFactory {

    private static final SimpleDateFormat filenameDateFormat = new SimpleDateFormat("YYYYMMdd-HHmmss");

    @Override
    public File recordingFileForTest(File vncRecordingDirectory, Description description, boolean succeeded) {
        final String prefix = succeeded ? "PASSED" : "FAILED";
        final String fileName = String.format("%s-%s-%s-%s.flv",
                prefix,
                description.getTestClass().getSimpleName(),
                description.getMethodName(),
                filenameDateFormat.format(new Date())
        );
        return new File(vncRecordingDirectory, fileName);
    }
}
