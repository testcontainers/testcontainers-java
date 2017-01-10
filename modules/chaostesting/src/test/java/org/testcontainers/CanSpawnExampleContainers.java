package org.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.github.dockerjava.api.DockerClient;

/**
 * Created by novy on 01.01.17.
 */
interface CanSpawnExampleContainers {

    default GenericContainer startedContainer() {
        final GenericContainer aContainer = new GenericContainer<>("alpine:latest")
                .withCommand("ping", "www.example.com");
        aContainer.start();
        return aContainer;
    }

    default GenericContainer stoppedContainer() {
        final GenericContainer aContainer = startedContainer();
        // this is to avoid running Resource reaper
        final DockerClient client = DockerClientFactory.instance().client();
        client.stopContainerCmd(aContainer.getContainerId()).exec();
        return aContainer;
    }

    default GenericContainer startedContainerWithName(String containerName) {
        final GenericContainer containerToRename = startedContainer();
        final DockerClient client = DockerClientFactory.instance().client();
        client.renameContainerCmd(containerToRename.getContainerId()).withName(containerName).exec();
        return containerToRename;
    }
}
