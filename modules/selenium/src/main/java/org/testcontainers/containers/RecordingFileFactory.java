package org.testcontainers.containers;

import org.junit.runner.Description;
import org.testcontainers.containers.VncRecordingContainer.VncRecordingFormat;

import java.io.File;

public interface RecordingFileFactory {

    @Deprecated
    default File recordingFileForTest(File vncRecordingDirectory, Description description, boolean succeeded) {
        return recordingFileForTest(vncRecordingDirectory, description.getTestClass().getSimpleName() + "-" + description.getMethodName(), succeeded);
    }

    default File recordingFileForTest(File vncRecordingDirectory, String prefix, boolean succeeded, VncRecordingFormat recordingFormat) {
        return recordingFileForTest(vncRecordingDirectory, prefix, succeeded);
    }

    File recordingFileForTest(File vncRecordingDirectory, String prefix, boolean succeeded);

}
