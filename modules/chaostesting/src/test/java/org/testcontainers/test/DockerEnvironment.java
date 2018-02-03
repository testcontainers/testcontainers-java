package org.testcontainers.test;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by novy on 31.12.16.
 */
class DockerEnvironment implements HasAccessToDockerClient {

    private final DockerClient delegate;

    DockerEnvironment() {
        this.delegate = dockerClient();
    }

    Collection<String> namesOfRunningContainers() {
        final List<Container> runningContainers = delegate.listContainersCmd().exec();
        return namesOf(runningContainers);
    }

    Collection<String> namesOfAllContainers() {
        final List<Container> runningContainers = delegate.listContainersCmd().withShowAll(true).exec();
        return namesOf(runningContainers);
    }

    private Collection<String> namesOf(Collection<Container> containers) {
        return containers.stream()
                .map(Container::getNames)
                .flatMap(Stream::of)
                .collect(Collectors.toSet());
    }

    ContainerDetails containerDetails(String byId) {
        final InspectContainerResponse inspectResponse = delegate.inspectContainerCmd(byId).exec();
        return new ContainerDetails(inspectResponse);
    }

    static class ContainerDetails {
        private final InspectContainerResponse response;

        ContainerDetails(InspectContainerResponse response) {
            this.response = response;
        }

        String name() {
            return response.getName();
        }

        boolean isRunning() {
            return response.getState().getRunning();
        }

        boolean isPaused() {
            return response.getState().getPaused();
        }
    }
}
