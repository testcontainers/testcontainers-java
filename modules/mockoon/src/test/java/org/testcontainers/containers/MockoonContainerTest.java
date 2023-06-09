package org.testcontainers.containers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Cleanup;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class MockoonContainerTest {

    private static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
        .parse("mockoon/cli:3.0.0");

    private static final String TEST_RESOURCE = "/demo.json";

    @Test
    public void shouldCallActualMockoonWithPath() throws Exception {
        Path tempFile = Files.createTempFile("testcontainer", "mockoon");
        Files.copy(
            MockoonContainerTest.class.getResourceAsStream(TEST_RESOURCE),
            tempFile,
            StandardCopyOption.REPLACE_EXISTING
        );

        try (MockoonContainer mockoon = new MockoonContainer(MOCKSERVER_IMAGE, tempFile)) {
            mockoon.start();

            assertThat(responseFromMockoon(mockoon, "/template"))
                .as("Mockoon returns correct result")
                .containsKey("Templating example");
        }
    }

    @Test
    public void shouldCallActualMockoonWithClasspathResource() throws Exception {
        try (MockoonContainer mockoon = new MockoonContainer(MOCKSERVER_IMAGE, TEST_RESOURCE)) {
            mockoon.start();

            assertThat(responseFromMockoon(mockoon, "/template"))
                .as("Mockoon returns correct result")
                .containsKey("Templating example");
        }
    }
    private static Map<String, Object> responseFromMockoon(MockoonContainer mockoon, String path) throws IOException {
        URLConnection urlConnection = new URL(mockoon.getEndpoint() + path).openConnection();
        @Cleanup
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(reader, new TypeReference<Map<String, Object>>() {});
    }
}
