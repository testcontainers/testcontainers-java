package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestcontainersExtensionTests {

    @Test
    void whenDisabledWithoutDockerAndDockerIsAvailableTestsAreEnabled() {
        ConditionEvaluationResult result = new TestTestcontainersExtension(true)
            .evaluateExecutionCondition(extensionContext(DisabledWithoutDocker.class));
        assertThat(result.isDisabled()).isFalse();
    }

    @Test
    void whenDisabledWithoutDockerAndDockerIsUnavailableTestsAreDisabled() {
        ConditionEvaluationResult result = new TestTestcontainersExtension(false)
            .evaluateExecutionCondition(extensionContext(DisabledWithoutDocker.class));
        assertThat(result.isDisabled()).isTrue();
    }

    @Test
    void whenEnabledWithoutDockerAndDockerIsAvailableTestsAreEnabled() {
        ConditionEvaluationResult result = new TestTestcontainersExtension(true)
            .evaluateExecutionCondition(extensionContext(EnabledWithoutDocker.class));
        assertThat(result.isDisabled()).isFalse();
    }

    @Test
    void whenEnabledWithoutDockerAndDockerIsUnavailableTestsAreEnabled() {
        ConditionEvaluationResult result = new TestTestcontainersExtension(false)
            .evaluateExecutionCondition(extensionContext(EnabledWithoutDocker.class));
        assertThat(result.isDisabled()).isFalse();
    }

    private ExtensionContext extensionContext(Class clazz) {
        ExtensionContext extensionContext = mock(ExtensionContext.class);
        when(extensionContext.getRequiredTestClass()).thenReturn(clazz);
        return extensionContext;
    }

    @Testcontainers(disabledWithoutDocker = true)
    static final class DisabledWithoutDocker {}

    @Testcontainers
    static final class EnabledWithoutDocker {}

    static final class TestTestcontainersExtension extends TestcontainersExtension {

        private final boolean dockerAvailable;

        private TestTestcontainersExtension(boolean dockerAvailable) {
            this.dockerAvailable = dockerAvailable;
        }

        boolean isDockerAvailable() {
            return dockerAvailable;
        }
    }
}
