package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ComposeContainerTest extends BaseComposeTest {

    @Rule
    // composeContainerConstructor {
    public ComposeContainer environment = new ComposeContainer(
        new File("src/test/resources/composev2/compose-test.yml")
    )
        .withExposedService("redis-1", REDIS_PORT)
        .withExposedService("db-1", 3306);

    // }

    @Override
    protected ComposeContainer getEnvironment() {
        return environment;
    }

    @Test
    public void testGetServiceHostAndPort() {
        // getServiceHostAndPort {
        String serviceHost = environment.getServiceHost("redis-1", REDIS_PORT);
        int serviceWithInstancePort = environment.getServicePort("redis-1", REDIS_PORT);
        // }

        assertThat(serviceHost).as("Service host is not blank").isNotBlank();
        assertThat(serviceWithInstancePort).as("Port is set for service with instance number").isNotNull();

        int serviceWithoutInstancePort = environment.getServicePort("redis", REDIS_PORT);
        assertThat(serviceWithoutInstancePort).as("Port is set for service with instance number").isNotNull();
        assertThat(serviceWithoutInstancePort).as("Service ports are the same").isEqualTo(serviceWithInstancePort);
    }

    @Test
    public void shouldRetrieveContainerByServiceName() {
        String existingServiceName = "db-1";
        Optional<ContainerState> result = environment.getContainerByServiceName(existingServiceName);
        assertThat(result)
            .as(String.format("Container should be found by service name %s", existingServiceName))
            .isPresent();
        assertThat(Collections.singletonList(3306))
            .as("Mapped port for result container was wrong, probably wrong container found")
            .isEqualTo(result.get().getExposedPorts());
    }

    @Test
    public void shouldReturnEmptyResultOnNoneExistingService() {
        String notExistingServiceName = "db-256";
        Optional<ContainerState> result = environment.getContainerByServiceName(notExistingServiceName);
        assertThat(result)
            .as(String.format("No container should be found under service name %s", notExistingServiceName))
            .isNotPresent();
    }

    @Test
    public void shouldCreateContainerWhenFileNotPrefixedWithPath() throws IOException {
        String validYaml =
            "version: '2.2'\n" +
            "services:\n" +
            "  http:\n" +
            "    build: .\n" +
            "    image: python:latest\n" +
            "    ports:\n" +
            "    - 8080:8080";

        File filePathNotStartWithDotSlash = new File("docker-compose-test.yml");
        filePathNotStartWithDotSlash.createNewFile();
        filePathNotStartWithDotSlash.deleteOnExit();
        Files.write(filePathNotStartWithDotSlash.toPath(), validYaml.getBytes(StandardCharsets.UTF_8));

        final DockerComposeContainer<?> dockerComposeContainer = new DockerComposeContainer<>(
            filePathNotStartWithDotSlash
        );
        assertThat(dockerComposeContainer).as("Container created using docker compose file").isNotNull();
    }
}
