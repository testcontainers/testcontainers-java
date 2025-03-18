package org.testcontainers.junit;

import io.restassured.RestAssured;
import org.junit.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.ContainerLaunchException;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ComposeContainerWithCopyFilesTest {

    @Test
    public void testShouldCopyAllFilesByDefault() throws IOException {
        try (
            ComposeContainer environment = new ComposeContainer(
                new File("src/test/resources/compose-file-copy-inclusions/compose.yml")
            )
                .withExposedService("app", 8080)
        ) {
            environment.start();

            String response = readStringFromURL(environment);
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
                .withCopyFilesInContainer("Dockerfile", "EnvVariableRestEndpoint.java", ".env")
        ) {
            environment.start();

            String response = readStringFromURL(environment);

            // The `test/.env` file is not copied, now so we get the original value
            assertThat(response).isEqualTo("MY_ENV_VARIABLE: original");
        }
    }

    @Test
    public void testWithFileCopyInclusionUsingDirectoryPath() throws IOException {
        try (
            // composeContainerWithCopyFiles {
            ComposeContainer environment = new ComposeContainer(
                new File("src/test/resources/compose-file-copy-inclusions/compose-test-only.yml")
            )
                .withExposedService("app", 8080)
                .withCopyFilesInContainer("Dockerfile", "EnvVariableRestEndpoint.java", "test")
            // }
        ) {
            environment.start();

            String response = readStringFromURL(environment);
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
                .withCopyFilesInContainer("Dockerfile", "EnvVariableRestEndpoint.java")
        ) {
            assertThatExceptionOfType(ContainerLaunchException.class)
                .isThrownBy(environment::start)
                .withMessageContaining("Container startup failed for image docker");
        }
    }

    private static String readStringFromURL(ComposeContainer container) throws IOException {
        Integer servicePort = container.getServicePort("app-1", 8080);
        String serviceHost = container.getServiceHost("app-1", 8080);
        String requestURL = "http://" + serviceHost + ":" + servicePort + "/env";
        return RestAssured.get(requestURL).thenReturn().body().asString();
    }
}
