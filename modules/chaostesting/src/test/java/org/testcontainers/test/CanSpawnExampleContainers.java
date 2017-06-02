package org.testcontainers.test;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.github.dockerjava.api.DockerClient;

/**
 * Created by novy on 01.01.17.
 */
interface CanSpawnExampleContainers extends HasAccessToDockerClient {

    default GenericContainer startedContainerWithCommand(String command) {
        final GenericContainer aContainer = new GenericContainer<>("alpine:3.6")
                .withCommand("sh", "-c", command);
        aContainer.start();
        return aContainer;
    }

    default GenericContainer startedContainer() {
        return startedContainerWithCommand("while true; do echo something; sleep 1; done");
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
        dockerClient().renameContainerCmd(containerToRename.getContainerId()).withName(containerName).exec();
        return containerToRename;
    }
}
