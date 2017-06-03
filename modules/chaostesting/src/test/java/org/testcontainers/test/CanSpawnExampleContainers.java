package org.testcontainers.test;

import org.testcontainers.containers.GenericContainer;

/**
 * Created by novy on 01.01.17.
 */
interface CanSpawnExampleContainers extends HasAccessToDockerClient {

    default GenericContainer startedContainer() {
        final GenericContainer aContainer = new GenericContainer<>("alpine:3.6")
                .withCommand("sh", "-c", "while true; do echo something; sleep 1; done");
        aContainer.start();
        return aContainer;
    }

    default GenericContainer startedContainerWithName(String containerName) {
        final GenericContainer containerToRename = startedContainer();
        dockerClient().renameContainerCmd(containerToRename.getContainerId()).withName(containerName).exec();
        return containerToRename;
    }
}
