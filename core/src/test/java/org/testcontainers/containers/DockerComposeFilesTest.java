package org.testcontainers.containers;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class DockerComposeFilesTest {

    @Test
    public void shouldGetDependencyImages() {
        DockerComposeFiles dockerComposeFiles = new DockerComposeFiles(
            Lists.newArrayList(new File("src/test/resources/docker-compose-imagename-parsing-v2.yml"))
        );
        assertThat(dockerComposeFiles.getDependencyImages())
            .containsExactlyInAnyOrder("postgres:latest", "redis:latest", "mysql:latest");
    }

    @Test
    public void shouldGetDependencyImagesWhenOverriding() {
        DockerComposeFiles dockerComposeFiles = new DockerComposeFiles(
            Lists.newArrayList(
                new File("src/test/resources/docker-compose-imagename-overriding-a.yml"),
                new File("src/test/resources/docker-compose-imagename-overriding-b.yml")
            )
        );
        assertThat(dockerComposeFiles.getDependencyImages())
            .containsExactlyInAnyOrder("alpine:3.16", "redis:b", "mysql:b", "aservice:latest");
    }
}
