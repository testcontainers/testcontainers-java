package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;


public class SharedContainersExtensionTests {

    @Test
    void whenDisabledWithoutDockerAndDockerIsAvailableTestsAreEnabled() {
        ConditionEvaluationResult result = new TestSharedContainersExtension(true)
            .evaluateExecutionCondition(extensionContext(DisabledWithoutDocker.class));
        assertThat(result.isDisabled()).isFalse();
    }

    @Test
    void whenDisabledWithoutDockerAndDockerIsUnavailableTestsAreDisabled() {
        ConditionEvaluationResult result = new TestSharedContainersExtension(false)
            .evaluateExecutionCondition(extensionContext(DisabledWithoutDocker.class));
        assertThat(result.isDisabled()).isTrue();
    }

    @Test
    void whenEnabledWithoutDockerAndDockerIsAvailableTestsAreEnabled() {
        ConditionEvaluationResult result = new TestSharedContainersExtension(true)
            .evaluateExecutionCondition(extensionContext(EnabledWithoutDocker.class));
        assertThat(result.isDisabled()).isFalse();
    }

    @Test
    void whenEnabledWithoutDockerAndDockerIsUnavailableTestsAreEnabled() {
        ConditionEvaluationResult result = new TestSharedContainersExtension(false)
            .evaluateExecutionCondition(extensionContext(EnabledWithoutDocker.class));
        assertThat(result.isDisabled()).isFalse();
    }

    private ExtensionContext extensionContext(Class<?> clazz) {
        ExtensionContext extensionContext = mock(ExtensionContext.class);
        doReturn(clazz).when(extensionContext).getRequiredTestClass();
        return extensionContext;
    }

    @SharedContainers(disabledWithoutDocker = true)
    static final class DisabledWithoutDocker {}

    @SharedContainers
    static final class EnabledWithoutDocker {}

    static final class TestSharedContainersExtension extends SharedContainersExtension {

        private final boolean dockerAvailable;

        private TestSharedContainersExtension(boolean dockerAvailable) {
            this.dockerAvailable = dockerAvailable;
        }

        @Override
        boolean isDockerAvailable() {
            return dockerAvailable;
        }
    }
}
