package org.testcontainers.junit.jqwik;

import lombok.Value;
import org.testcontainers.lifecycle.TestDescription;

@Value
class TestcontainersTestDescription implements TestDescription {
    String testId;
    String filesystemFriendlyName;
}
