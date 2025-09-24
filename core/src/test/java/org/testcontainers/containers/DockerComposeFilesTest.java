package org.testcontainers.containers;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class DockerComposeFilesTest {

    @Test
    void shouldGetDependencyImages() {
        DockerComposeFiles dockerComposeFiles = new DockerComposeFiles(
            Lists.newArrayList(new File("src/test/resources/docker-compose-imagename-parsing-v2.yml"))
        );
        assertThat(dockerComposeFiles.getDependencyImages())
            .containsExactlyInAnyOrder("postgres:latest", "redis:latest", "mysql:latest");
    }

    @Test
    void shouldGetDependencyImagesWhenOverriding() {
        DockerComposeFiles dockerComposeFiles = new DockerComposeFiles(
            Lists.newArrayList(
                new File("src/test/resources/docker-compose-imagename-overriding-a.yml"),
                new File("src/test/resources/docker-compose-imagename-overriding-b.yml")
            )
        );
        assertThat(dockerComposeFiles.getDependencyImages())
            .containsExactlyInAnyOrder("alpine:3.17", "redis:b", "mysql:b", "aservice:latest");
    }
}
