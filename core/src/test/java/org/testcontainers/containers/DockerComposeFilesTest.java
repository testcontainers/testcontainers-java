package org.testcontainers.containers;


import com.google.common.collect.Lists;
import java.io.File;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class DockerComposeFilesTest {
    @Test
    public void shouldGetDependencyImages() {
        DockerComposeFiles dockerComposeFiles = new DockerComposeFiles(Lists.newArrayList(new File("src/test/resources/docker-compose-imagename-parsing-v2.yml")));
        Assertions.assertThat(dockerComposeFiles.getDependencyImages())
            .containsExactlyInAnyOrder("postgres:latest", "redis:latest", "mysql:latest");
    }

    @Test
    public void shouldGetDependencyImagesWhenOverriding() {
        DockerComposeFiles dockerComposeFiles = new DockerComposeFiles(
            Lists.newArrayList(new File("src/test/resources/docker-compose-imagename-overriding-a.yml"),
                               new File("src/test/resources/docker-compose-imagename-overriding-b.yml")));
        Assertions.assertThat(dockerComposeFiles.getDependencyImages())
            .containsExactlyInAnyOrder("alpine:3.14", "redis:b", "mysql:b", "aservice:latest");
    }
}
