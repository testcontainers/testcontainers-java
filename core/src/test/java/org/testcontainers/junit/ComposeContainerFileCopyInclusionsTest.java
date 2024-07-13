package org.testcontainers.junit;

import org.junit.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.ContainerLaunchException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ComposeContainerFileCopyInclusionsTest {

    @Test
    public void testShouldCopyAllFilesByDefault() throws IOException {
        try (
            ComposeContainer environment = new ComposeContainer(
                new File("src/test/resources/compose-file-copy-inclusions/compose.yml")
            )
                .withExposedService("app", 8080)
        ) {
            environment.start();

            Integer servicePort = environment.getServicePort("app-1", 8080);
            String serviceHost = environment.getServiceHost("app-1", 8080);
            String response = readStringFromURL("http://" + serviceHost + ":" + servicePort + "/env");
            assertThat(response).isEqualTo("MY_ENV_VARIABLE: override");
        }
    }

    @Test
    public void testWithFileCopyInclusionUsingFilePath() throws IOException {
        try (
            ComposeContainer environment = new ComposeContainer(
                new File("src/test/resources/compose-file-copy-inclusions/compose-root-only.yml")
            )
                .withExposedService("app", 8080)
                .withFileCopyInclusions("Dockerfile", "EnvVariableRestEndpoint.java", ".env")
        ) {
            environment.start();

            Integer servicePort = environment.getServicePort("app-1", 8080);
            String serviceHost = environment.getServiceHost("app-1", 8080);
            String response = readStringFromURL("http://" + serviceHost + ":" + servicePort + "/env");

            // The `test/.env` file is not copied, now so we get the original value
            assertThat(response).isEqualTo("MY_ENV_VARIABLE: original");
        }
    }

    @Test
    public void testWithFileCopyInclusionUsingDirectoryPath() throws IOException {
        try (
            ComposeContainer environment = new ComposeContainer(
                new File("src/test/resources/compose-file-copy-inclusions/compose-test-only.yml")
            )
                .withExposedService("app", 8080)
                .withFileCopyInclusions("Dockerfile", "EnvVariableRestEndpoint.java", "test")
        ) {
            environment.start();

            Integer servicePort = environment.getServicePort("app-1", 8080);
            String serviceHost = environment.getServiceHost("app-1", 8080);
            String response = readStringFromURL("http://" + serviceHost + ":" + servicePort + "/env");
            // The test directory (with its contents) is copied, so we get the override
            assertThat(response).isEqualTo("MY_ENV_VARIABLE: override");
        }
    }

    @Test
    public void testShouldNotBeAbleToStartIfNeededEnvFileIsNotCopied() {
        try (
            ComposeContainer environment = new ComposeContainer(
                new File("src/test/resources/compose-file-copy-inclusions/compose-test-only.yml")
            )
                .withExposedService("app", 8080)
                .withFileCopyInclusions("Dockerfile", "EnvVariableRestEndpoint.java")
        ) {
            assertThatExceptionOfType(ContainerLaunchException.class)
                .isThrownBy(environment::start)
                .withMessage("Container startup failed for image docker:24.0.2");
        }
    }

    private static String readStringFromURL(String requestURL) throws IOException {
        try (Scanner scanner = new Scanner(new URL(requestURL).openStream(), StandardCharsets.UTF_8.toString())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
}
