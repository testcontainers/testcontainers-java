package org.testcontainers;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.runner.Description;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.io.File;

/**
 * Created by novy on 01.01.17.
 */
interface CanSpawnExampleContainers {

    default GenericContainer startedContainer() {
        final GenericContainer aContainer = new GenericContainer<>("ubuntu:latest")
                .withCommand("bash", "-c", "while true; do echo something; sleep 1; done");
        aContainer.start();
        return aContainer;
    }

    default GenericContainer stoppedContainer() {
        final GenericContainer aContainer = startedContainer();
        // this is to avoid running Resource reaper
        aContainer.getDockerClient().stopContainerCmd(aContainer.getContainerId()).exec();
        return aContainer;
    }

    @SneakyThrows
    default void startContainerWithNameContaining(String partOfContainerName) {
        final File temporaryFile = File.createTempFile("temp-compose", ".yml");
        final ImmutableList<String> composeContent = ImmutableList.of(
                partOfContainerName + ":",
                "  image: ubuntu:latest",
                "  command: [bash, -c, \"while true; do echo something; sleep 1; done\"]"
        );
        FileUtils.writeLines(
                temporaryFile, composeContent
        );

        final DockerComposeContainer composeContainer = new DockerComposeContainer<>(temporaryFile);
        composeContainer.starting(Description.EMPTY);
    }
}
