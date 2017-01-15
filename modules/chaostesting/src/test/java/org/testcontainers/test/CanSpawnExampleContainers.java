package org.testcontainers.test;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.github.dockerjava.api.DockerClient;

/**
 * Created by novy on 01.01.17.
 */
interface CanSpawnExampleContainers {

    default GenericContainer startedContainer() {
        final GenericContainer aContainer = new GenericContainer<>("alpine:latest")
                .withCommand("sh", "-c", "while true; do echo something; sleep 1; done");
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
