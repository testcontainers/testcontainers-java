package org.testcontainers.junit.jupiter;

import lombok.RequiredArgsConstructor;
import org.testcontainers.lifecycle.TestDescription;

@RequiredArgsConstructor
public class TestcontainersTestDescription implements TestDescription {
    private final String testId;
    private final String filesystemFriendlyName;

    @Override
    public String getTestId() {
        return testId;
    }

    @Override
    public String getFilesystemFriendlyName() {
        return filesystemFriendlyName;
    }
}
