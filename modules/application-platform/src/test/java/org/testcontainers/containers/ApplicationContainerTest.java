package org.testcontainers.containers;

import lombok.NonNull;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationContainerTest {

    private static ApplicationContainer testContainer;

    @Test
    public void testNormalizePath() {
        String expected, actual;

        expected = "/path/to/application";
        actual = ApplicationContainer.normalizePath("path", "to", "application");
        assertThat(actual).isEqualTo(expected);

        actual = ApplicationContainer.normalizePath("path/to", "application");
        assertThat(actual).isEqualTo(expected);

        actual = ApplicationContainer.normalizePath("path/to/application");
        assertThat(actual).isEqualTo(expected);

        actual = ApplicationContainer.normalizePath("path/", "to/", "application/");
        assertThat(actual).isEqualTo(expected);

        actual = ApplicationContainer.normalizePath("path/", "/to/", "/application/");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void httpPortMapping() {
        List<Integer> expected, actual;

        expected = List.of(8080, 9080, 9443);

        // Test expose ports, then add httpPort
        testContainer = new ApplicationContainerStub(DockerImageName.parse("open-liberty:kernel-slim-java11-openj9"));
        testContainer.withExposedPorts(9080, 9443);
        testContainer.withHttpPort(8080);

        actual = testContainer.getExposedPorts();
        assertThat(actual).containsExactlyElementsOf(expected);

        // Test httpPort then expose ports
        testContainer = new ApplicationContainerStub(DockerImageName.parse("open-liberty:kernel-slim-java11-openj9"));
        testContainer.withHttpPort(8080);
        testContainer.withExposedPorts(9080, 9443);

        actual = testContainer.getExposedPorts();
        assertThat(actual).containsExactlyElementsOf(expected);

        //Test httpPort then set exposed ports
        testContainer = new ApplicationContainerStub(DockerImageName.parse("open-liberty:kernel-slim-java11-openj9"));
        testContainer.withHttpPort(8080);
        testContainer.setExposedPorts(List.of(9080, 9443));

        actual = testContainer.getExposedPorts();
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    static class ApplicationContainerStub extends ApplicationContainer {

        public ApplicationContainerStub(@NonNull Future<String> image) {
            super(image);
        }

        public ApplicationContainerStub(DockerImageName dockerImageName) {
            super(dockerImageName);
        }

        @Override
        protected Duration getDefaultWaitTimeout() {
            return Duration.ofSeconds(1);
        }

        @Override
        protected String getApplicationInstallDirectory() {
            return "null";
        }
    }
}
