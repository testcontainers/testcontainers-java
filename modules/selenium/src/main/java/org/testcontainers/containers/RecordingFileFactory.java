package org.testcontainers.containers;

import org.junit.runner.Description;

import java.io.File;

public interface RecordingFileFactory {
    File recordingFileForTest(File vncRecordingDirectory, Description description, boolean succeeded);
}
