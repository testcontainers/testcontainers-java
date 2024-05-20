package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.Optional;

public class EnabledIfDockerAvailableCondition implements ExecutionCondition {

    private final DockerAvailableDetector dockerDetector = new DockerAvailableDetector();

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return findAnnotation(context)
            .map(this::evaluate)
            .orElseThrow(() -> new ExtensionConfigurationException("@EnabledIfDockerAvailable not found"));
    }

    boolean isDockerAvailable() {
        return this.dockerDetector.isDockerAvailable();
    }

    private ConditionEvaluationResult evaluate(EnabledIfDockerAvailable testcontainers) {
        if (isDockerAvailable()) {
            return ConditionEvaluationResult.enabled("Docker is available");
        }
        return ConditionEvaluationResult.disabled("Docker is not available");
    }

    private Optional<EnabledIfDockerAvailable> findAnnotation(ExtensionContext context) {
        Optional<ExtensionContext> current = Optional.of(context);
        while (current.isPresent()) {
            Optional<EnabledIfDockerAvailable> enabledIfDockerAvailable = AnnotationSupport.findAnnotation(
                current.get().getRequiredTestClass(),
                EnabledIfDockerAvailable.class
            );
            if (enabledIfDockerAvailable.isPresent()) {
                return enabledIfDockerAvailable;
            }
            current = current.get().getParent();
        }
        return Optional.empty();
    }
}
