package org.testcontainers.containers;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.testcontainers.containers.VncRecordingContainer.VncRecordingFormat;

public class DefaultRecordingFileFactory implements RecordingFileFactory {

    private static final SimpleDateFormat filenameDateFormat = new SimpleDateFormat("YYYYMMdd-HHmmss");
    private static final String PASSED = "PASSED";
    private static final String FAILED = "FAILED";
    private static final String FILENAME_FORMAT = "%s-%s-%s.%s";


    @Override
    public File recordingFileForTest(File vncRecordingDirectory, String prefix, boolean succeeded) {
        return recordingFileForTest(vncRecordingDirectory, prefix, succeeded, VncRecordingContainer.DEFAULT_RECORDING_FORMAT);
    }

    @Override
    public File recordingFileForTest(File vncRecordingDirectory, String prefix, boolean succeeded, VncRecordingFormat recordingFormat) {
        final String resultMarker = succeeded ? PASSED : FAILED;
        final String fileName = String.format(FILENAME_FORMAT,
            resultMarker,
            prefix,
            filenameDateFormat.format(new Date()),
            recordingFormat.getFilenameExtension()
        );
        return new File(vncRecordingDirectory, fileName);
    }
}
