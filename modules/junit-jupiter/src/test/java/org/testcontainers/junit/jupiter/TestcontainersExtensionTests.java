package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.utility.MockTestcontainersConfigurationExtension;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockTestcontainersConfigurationExtension.class)
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

    @ParameterizedTest
    @MethodSource("disabledWithoutDockerAllProvider")
    void whenInvokedByExtendWith(Boolean config, boolean dockerAvailable, boolean expectedDisabled, String expectedReason) {
        testDisabledEvaluation(config, dockerAvailable, expectedDisabled, expectedReason, WithoutAnnotation.class);
    }

    @ParameterizedTest
    @MethodSource("disabledWithoutDockerConfigurationSpecifiedProvider")
    void whenInvokedByExtendWithTestcontainersAnnotationIgnored(Boolean config,
                                                                boolean dockerAvailable,
                                                                boolean expectedDisabled,
                                                                String expectedReason) {
        testDisabledEvaluation(config, dockerAvailable, expectedDisabled, expectedReason, DisabledWithoutDocker.class);
        testDisabledEvaluation(config, dockerAvailable, expectedDisabled, expectedReason, EnabledWithoutDocker.class);
    }

    private void testDisabledEvaluation(Boolean config,
                                        boolean dockerAvailable,
                                        boolean expectedDisabled,
                                        String expectedReason,
                                        Class<?> annotationClass) {
        doReturn(config).when(TestcontainersConfiguration.getInstance()).disabledWithoutDocker();
        ConditionEvaluationResult result = new TestTestcontainersExtension(dockerAvailable)
            .evaluateExecutionCondition(extensionContext(annotationClass));
        assertEquals(expectedDisabled, result.isDisabled());
        assertEquals(expectedReason, result.getReason().get());
    }

    private ExtensionContext extensionContext(Class<?> clazz) {
        ExtensionContext extensionContext = mock(ExtensionContext.class);
        when(extensionContext.getTestClass()).thenReturn(Optional.ofNullable(clazz));
        return extensionContext;
    }

    static Stream<Arguments> disabledWithoutDockerAllProvider() {
        return Stream.concat(
            disabledWithoutDockerConfigurationNotSpecifiedProvider(),
            disabledWithoutDockerConfigurationSpecifiedProvider());
    }

    static Stream<Arguments> disabledWithoutDockerConfigurationNotSpecifiedProvider() {
        return Stream.of(
            arguments(null, true, false, "disabledWithoutDocker is not set, assume enabled"),
            arguments(null, false, false, "disabledWithoutDocker is not set, assume enabled")
        );
    }

    static Stream<Arguments> disabledWithoutDockerConfigurationSpecifiedProvider() {
        return Stream.of(
            arguments(true, true, false, "Docker is available"),
            arguments(true, false, true, "disabledWithoutDocker is true and Docker is not available"),
            arguments(false, true, false, "disabledWithoutDocker config is false"),
            arguments(false, false, false, "disabledWithoutDocker config is false")
        );
    }

    @Testcontainers(disabledWithoutDocker = true)
    static final class DisabledWithoutDocker {

    }

    @Testcontainers
    static final class EnabledWithoutDocker {
    }

    static final class WithoutAnnotation {
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
