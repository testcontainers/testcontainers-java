package org.testcontainers.utility;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Regression tests for the JDK used by the nightly Gradle wrapper update. No Docker or GitHub access is needed. */
class GradleWrapperWorkflowTest {

    @Test
    void setsUpJavaAfterCheckoutAndBeforeUpdatingTheWrapper() throws IOException {
        List<Map<String, Object>> steps = workflowSteps();

        int checkout = actionIndex(steps, "actions/checkout@");
        int setupJava = actionIndex(steps, "./.github/actions/setup-java");
        int updateWrapper = actionIndex(steps, "gradle-update/update-gradle-wrapper-action@");

        assertThat(setupJava).as("local Java setup requires a checked out repository").isGreaterThan(checkout);
        assertThat(setupJava).as("the wrapper updater must see the configured JDK").isLessThan(updateWrapper);
    }

    @Test
    void doesNotSkipOrIgnoreJavaSetupFailures() throws IOException {
        List<Map<String, Object>> steps = workflowSteps();
        Map<String, Object> setupJava = steps.get(actionIndex(steps, "./.github/actions/setup-java"));

        assertThat(setupJava).as("Java setup must run on every nightly update").doesNotContainKey("if");
        assertThat(setupJava)
            .as("a failed setup must stop the job instead of using the runner's default JDK")
            .doesNotContainKey("continue-on-error");
    }

    @Test
    void configuresTemurin17InsteadOfTheRunnerDefault() throws IOException {
        List<Map<String, Object>> steps = workflowSteps();
        Map<String, Object> setupJava = steps.get(actionIndex(steps, "./.github/actions/setup-java"));
        Map<String, Object> action = readYaml(".github/actions/setup-java/action.yml");
        Map<String, Object> javaVersionInput = map(map(action.get("inputs")).get("java-version"));
        Map<String, Object> overrides = setupJava.containsKey("with")
            ? map(setupJava.get("with"))
            : Collections.emptyMap();

        assertThat(overrides.getOrDefault("java-version", javaVersionInput.get("default"))).isEqualTo("17");

        List<Map<String, Object>> actionSteps = steps(map(action.get("runs")));
        Map<String, Object> installJava = actionSteps.get(actionIndex(actionSteps, "actions/setup-java@"));
        Map<String, Object> options = map(installJava.get("with"));
        assertThat(options)
            .containsEntry("java-version", "${{ inputs.java-version }}")
            .containsEntry("distribution", "temurin");
    }

    private static List<Map<String, Object>> workflowSteps() throws IOException {
        Map<String, Object> workflow = readYaml(".github/workflows/update-gradle-wrapper.yml");
        return steps(map(map(workflow.get("jobs")).get("update-gradle-wrapper")));
    }

    private static int actionIndex(List<Map<String, Object>> steps, String action) {
        for (int i = 0; i < steps.size(); i++) {
            if (String.valueOf(steps.get(i).get("uses")).startsWith(action)) {
                return i;
            }
        }
        throw new AssertionError("Missing action: " + action);
    }

    private static Map<String, Object> readYaml(String relativePath) throws IOException {
        // Gradle runs tests in core/, while IDEs may use the repository root.
        Path root = Paths.get("").toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve(relativePath))) {
            root = root.getParent();
        }
        assertThat(root).as("repository containing %s", relativePath).isNotNull();
        try (InputStream input = Files.newInputStream(root.resolve(relativePath))) {
            return new Yaml().load(input);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> steps(Map<String, Object> parent) {
        return (List<Map<String, Object>>) parent.get("steps");
    }
}
