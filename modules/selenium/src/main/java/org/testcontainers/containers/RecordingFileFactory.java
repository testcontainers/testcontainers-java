package org.testcontainers.containers;

import org.junit.runner.Description;

import java.io.File;

public interface RecordingFileFactory {

    @Deprecated
    default File recordingFileForTest(File vncRecordingDirectory, Description description, boolean succeeded) {
        return recordingFileForTest(vncRecordingDirectory, description.getTestClass().getSimpleName() + "-" + description.getMethodName(), succeeded);
    }

    File recordingFileForTest(File vncRecordingDirectory, String prefix, boolean succeeded);
}
