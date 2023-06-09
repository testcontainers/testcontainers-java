package org.testcontainers.containers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Cleanup;
import org.junit.Rule;
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
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class MockoonContainerTest {

    // initialization {
    public static final DockerImageName MOCKOON_IMAGE = DockerImageName
        .parse("mockoon/cli:3.0.0");

    @Rule
    public MockoonContainer mockoonFromClasspath = new MockoonContainer(
        MOCKOON_IMAGE,
        "/demo.json" // Classpath Resource
    );
    // }

    public static final String TEST_CLASSPATH_RESOURCE = "/demo.json";

    @Test
    public void shouldCallActualMockoonWithClasspathResource() throws Exception {
        // testSimpleExpectation {

        try (MockoonContainer mockoon = new MockoonContainer(MOCKOON_IMAGE, TEST_CLASSPATH_RESOURCE)) {
            mockoon.start();

            assertThat(jsonResponseFromMockoon(mockoon, "/template"))
                .as("Mockoon returns expected result")
                .containsKey("Templating example");
        }

        // }
    }

    @Test
    public void shouldCallActualMockoonWithPath() throws Exception {
        Path tempFile = Files.createTempFile("testcontainer", "mockoon");
        Files.copy(
            Objects.requireNonNull(MockoonContainerTest.class.getResourceAsStream(TEST_CLASSPATH_RESOURCE)),
            tempFile,
            StandardCopyOption.REPLACE_EXISTING
        );

        try (MockoonContainer mockoon = new MockoonContainer(MOCKOON_IMAGE, tempFile)) {
            mockoon.start();

            assertThat(jsonResponseFromMockoon(mockoon, "/template"))
                .as("Mockoon returns expected result")
                .containsKey("Templating example");
        }
    }

    private static Map<String, Object> jsonResponseFromMockoon(MockoonContainer mockoon, String path) throws IOException {
        URLConnection urlConnection = new URL(mockoon.getEndpoint() + path).openConnection();
        @Cleanup
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(reader, new TypeReference<Map<String, Object>>() {});
    }
}
