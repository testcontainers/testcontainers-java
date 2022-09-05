package org.testcontainers.junit4;

import lombok.Value;
import org.testcontainers.lifecycle.TestDescription;

@Value
class TestcontainersTestDescription implements TestDescription {

    String testId;

    String filesystemFriendlyName;
}
