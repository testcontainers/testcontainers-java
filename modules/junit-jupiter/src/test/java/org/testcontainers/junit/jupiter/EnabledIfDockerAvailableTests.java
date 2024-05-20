package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EnabledIfDockerAvailableTests {

    @Test
    void whenDockerIsAvailableTestsAreEnabled() {
        ConditionEvaluationResult result = new TestEnabledIfDockerAvailableCondition(true)
            .evaluateExecutionCondition(extensionContext(DisabledWithoutDocker.class));
        assertThat(result.isDisabled()).isFalse();
    }

    @Test
    void whenDockerIsUnavailableTestsAreDisabled() {
        ConditionEvaluationResult result = new TestEnabledIfDockerAvailableCondition(false)
            .evaluateExecutionCondition(extensionContext(DisabledWithoutDocker.class));
        assertThat(result.isDisabled()).isTrue();
    }

    private ExtensionContext extensionContext(Class clazz) {
        ExtensionContext extensionContext = mock(ExtensionContext.class);
        when(extensionContext.getRequiredTestClass()).thenReturn(clazz);
        return extensionContext;
    }

    @EnabledIfDockerAvailable
    static final class DisabledWithoutDocker {}

    static final class TestEnabledIfDockerAvailableCondition extends EnabledIfDockerAvailableCondition {

        private final boolean dockerAvailable;

        private TestEnabledIfDockerAvailableCondition(boolean dockerAvailable) {
            this.dockerAvailable = dockerAvailable;
        }

        boolean isDockerAvailable() {
            return dockerAvailable;
        }
    }
}
