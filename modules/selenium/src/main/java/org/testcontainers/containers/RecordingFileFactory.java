package org.testcontainers.containers;

import org.testcontainers.containers.VncRecordingContainer.VncRecordingFormat;

import java.io.File;

public interface RecordingFileFactory {
    default File recordingFileForTest(
        File vncRecordingDirectory,
        String prefix,
        boolean succeeded,
        VncRecordingFormat recordingFormat
    ) {
        return recordingFileForTest(vncRecordingDirectory, prefix, succeeded);
    }

    File recordingFileForTest(File vncRecordingDirectory, String prefix, boolean succeeded);
}
