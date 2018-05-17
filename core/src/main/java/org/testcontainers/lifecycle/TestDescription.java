package org.testcontainers.lifecycle;

import java.util.Optional;

public interface TestDescription {

    String getTestId();

    default String getDisplayName() {
        return getTestId();
    }

    default Optional<String[]> getNameParts() {
        return Optional.empty();
    }

    default Optional<String> getFilesystemFriendlyName() {
        return Optional.empty();
    }
}
