package org.testcontainers.junit;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by rnorth on 08/08/2015.
 */
@Disabled
class DockerComposeContainerTest extends BaseDockerComposeTest {

    @AutoClose
    public DockerComposeContainer environment = new DockerComposeContainer(
        DockerImageName.parse("docker/compose:1.29.2"),
        new File("src/test/resources/compose-test.yml")
    )
        .withExposedService("redis_1", REDIS_PORT)
        .withExposedService("db_1", 3306);

    DockerComposeContainerTest() {
        environment.start();
    }

    @Override
    protected DockerComposeContainer getEnvironment() {
        return environment;
    }

    @Test
    void testGetServicePort() {
        int serviceWithInstancePort = environment.getServicePort("redis_1", REDIS_PORT);
        assertThat(serviceWithInstancePort).as("Port is set for service with instance number").isNotNull();
        int serviceWithoutInstancePort = environment.getServicePort("redis", REDIS_PORT);
        assertThat(serviceWithoutInstancePort).as("Port is set for service with instance number").isNotNull();
        assertThat(serviceWithoutInstancePort).as("Service ports are the same").isEqualTo(serviceWithInstancePort);
    }

    @Test
    void shouldRetrieveContainerByServiceName() {
        String existingServiceName = "db_1";
        Optional<ContainerState> result = environment.getContainerByServiceName(existingServiceName);

        assertThat(result)
            .as(String.format("Container should be found by service name %s", existingServiceName))
            .isPresent();
        assertThat(Collections.singletonList(3306))
            .as("Mapped port for result container was wrong, probably wrong container found")
            .isEqualTo(result.get().getExposedPorts());
    }

    @Test
    void shouldRetrieveContainerByServiceNameWithoutNumberedSuffix() {
        String existingServiceName = "db";
        Optional<ContainerState> result = environment.getContainerByServiceName(existingServiceName);

        assertThat(result)
            .as(String.format("Container should be found by service name %s", existingServiceName))
            .isPresent();
        assertThat(result.get().getExposedPorts())
            .as("Mapped port for result container was wrong, perhaps wrong container was found")
            .isEqualTo(Collections.singletonList(3306));
    }

    @Test
    void shouldReturnEmptyResultOnNoneExistingService() {
        String notExistingServiceName = "db_256";
        Optional<ContainerState> result = environment.getContainerByServiceName(notExistingServiceName);
        assertThat(result)
            .as(String.format("No container should be found under service name %s", notExistingServiceName))
            .isNotPresent();
    }

    @Test
    void shouldCreateContainerWhenFileNotPrefixedWithPath() throws IOException {
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
            DockerImageName.parse("docker/compose:debian-1.29.2"),
            filePathNotStartWithDotSlash
        );
        assertThat(dockerComposeContainer).as("Container could not be created using docker compose file").isNotNull();
    }
}
