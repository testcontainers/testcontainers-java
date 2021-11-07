package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestcontainersExtensionTests {

    @Test
    void whenDisabledWithoutDockerAndDockerIsAvailableTestsAreEnabled() {
        ConditionEvaluationResult result = new TestTestcontainersExtension(true)
            .evaluateExecutionCondition(extensionContext(DisabledWithoutDocker.class));
        assertFalse(result.isDisabled());
    }

    @Test
    void whenDisabledWithoutDockerAndDockerIsUnavailableTestsAreDisabled() {
        ConditionEvaluationResult result = new TestTestcontainersExtension(false)
            .evaluateExecutionCondition(extensionContext(DisabledWithoutDocker.class));
        assertTrue(result.isDisabled());
    }

    @Test
    void whenDisabledWithoutDockerAndDockerIsAvailableTestsAreEnabledUsingExtendsWith() {
        ConditionEvaluationResult result = new TestTestcontainersExtension(true)
            .evaluateExecutionCondition(extensionContext(DisabledExtendWithAndAnnotation.class));
        assertFalse(result.isDisabled());
    }

    @Test
    void whenDisabledWithoutDockerAndDockerIsUnavailableTestsAreDisabledUsingExtendsWith() {
        ConditionEvaluationResult result = new TestTestcontainersExtension(false)
            .evaluateExecutionCondition(extensionContext(DisabledExtendWithAndAnnotation.class));
        assertTrue(result.isDisabled());
    }

    @Test
    void whenEnabledWithoutDockerAndDockerIsAvailableTestsAreEnabled() {
        ConditionEvaluationResult result = new TestTestcontainersExtension(true)
            .evaluateExecutionCondition(extensionContext(EnabledWithoutDocker.class));
        assertFalse(result.isDisabled());
    }

    @Test
    void whenEnabledWithoutDockerAndDockerIsUnavailableTestsAreEnabled() {
        ConditionEvaluationResult result = new TestTestcontainersExtension(false)
            .evaluateExecutionCondition(extensionContext(EnabledWithoutDocker.class));
        assertFalse(result.isDisabled());
    }

    @Test
    void whenEnabledWithoutDockerAndDockerIsAvailableTestsAreEnabledUsingExtendsWith() {
        ConditionEvaluationResult result = new TestTestcontainersExtension(true)
            .evaluateExecutionCondition(extensionContext(EnabledExtendWith.class));
        assertFalse(result.isDisabled());
    }

    @Test
    void whenEnabledWithoutDockerAndDockerIsUnavailableTestsAreEnabledUsingExtendsWith() {
        ConditionEvaluationResult result = new TestTestcontainersExtension(false)
            .evaluateExecutionCondition(extensionContext(EnabledExtendWith.class));
        assertFalse(result.isDisabled());
    }

    @Test
    void whenEnabledWithoutDockerAndDockerIsAvailableTestsAreEnabledUsingExtendsWithAndAnnotation() {
        ConditionEvaluationResult result = new TestTestcontainersExtension(true)
            .evaluateExecutionCondition(extensionContext(EnabledExtendWithAndAnnotation.class));
        assertFalse(result.isDisabled());
    }

    @Test
    void whenEnabledWithoutDockerAndDockerIsUnavailableTestsAreEnabledUsingExtendsWithAndAnnotation() {
        ConditionEvaluationResult result = new TestTestcontainersExtension(false)
            .evaluateExecutionCondition(extensionContext(EnabledExtendWithAndAnnotation.class));
        assertFalse(result.isDisabled());
    }

    private ExtensionContext extensionContext(Class<?> clazz) {
        ExtensionContext extensionContext = mock(ExtensionContext.class);
        when(extensionContext.getTestClass()).thenReturn(Optional.ofNullable(clazz));
        return extensionContext;
    }

    @Testcontainers(disabledWithoutDocker = true)
    static final class DisabledWithoutDocker {

    }

    @Testcontainers
    static final class EnabledWithoutDocker {

    }

    @ExtendWith(TestcontainersExtension.class)
    static final class EnabledExtendWith {
    }

    @ExtendWith(TestcontainersExtension.class)
    @Testcontainers
    static final class EnabledExtendWithAndAnnotation {
    }

    @ExtendWith(TestcontainersExtension.class)
    @Testcontainers(disabledWithoutDocker = true)
    static final class DisabledExtendWithAndAnnotation {
    }

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
