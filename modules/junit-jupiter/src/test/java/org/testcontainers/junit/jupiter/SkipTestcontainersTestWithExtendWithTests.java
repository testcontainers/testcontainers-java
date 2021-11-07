package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SkipTestcontainersTestWithExtendWithTests.DockerNotAvailableTestcontainersExtension.class)
@Testcontainers(disabledWithoutDocker = true)
class SkipTestcontainersTestWithExtendWithTests {
    @Test
    void shouldBeSkipped() {
        fail("This test has to be skipped");
    }

    static final class DockerNotAvailableTestcontainersExtension extends TestcontainersExtension {
        boolean isDockerAvailable() {
            return false;
        }
    }
}
