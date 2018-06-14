package org.testcontainers.lifecycle;

public interface TestDescription {

    String getTestId();

    String getFilesystemFriendlyName();
}
