package org.testcontainers.executables;

import com.github.dockerjava.api.DockerClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerStatus;

import static org.testcontainers.containers.BindMode.READ_WRITE;

/**
 * Created by novy on 31.12.16.
 */
@Slf4j
class PumbaContainer extends GenericContainer<PumbaContainer> {

    private static final String PUMBA_DOCKER_IMAGE = "gaiaadm/pumba:0.4.7";
    private static final String IP_ROUTE_DOCKER_IMAGE = "gaiadocker/iproute2:3.3";

    private static final String DOCKER_SOCKET_HOST_PATH = "//var/run/docker.sock";
    private static final String DOCKER_SOCKET_CONTAINER_PATH = "/docker.sock";

    PumbaContainer(String pumbaCommandToExecute) {
        super(buildPumbaDockerImage());
        setCommand(pumbaCommandToExecute);
        doNotWaitForStartupAtAll();
        mountDockerSocket();
        fetchIPRouteImage();
        setupLogging();
    }

    private void doNotWaitForStartupAtAll() {
        setStartupCheckStrategy(new FailOnlyOnErrorExitCode());
    }

    private void mountDockerSocket() {
        addFileSystemBind(DOCKER_SOCKET_HOST_PATH, DOCKER_SOCKET_CONTAINER_PATH, READ_WRITE);
        addEnv("DOCKER_HOST", String.format("unix://%s", DOCKER_SOCKET_CONTAINER_PATH));
    }

    @SneakyThrows
    private void fetchIPRouteImage() {
        new RemoteDockerImage(IP_ROUTE_DOCKER_IMAGE).get();
    }

    private void setupLogging() {
        withLogConsumer(new Slf4jLogConsumer(log));
    }

    private static ImageFromDockerfile buildPumbaDockerImage() {
        return new ImageFromDockerfile("testcontainers/pumba")
                .withDockerfileFromBuilder(builder -> builder
                        .from(PUMBA_DOCKER_IMAGE)
                        .run("echo -n > /docker_entrypoint.sh")
                        .run("echo '#!/bin/sh' >> /docker_entrypoint.sh")
                        .run("echo 'set -e' >> /docker_entrypoint.sh")
                        .run("echo 'exec gosu root:root \"$@\"' >> /docker_entrypoint.sh")
                );
    }

    private static class FailOnlyOnErrorExitCode extends StartupCheckStrategy {

        @Override
        public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
            return exitedWithError(dockerClient, containerId) ?
                    StartupStatus.FAILED :
                    StartupStatus.SUCCESSFUL;
        }

        private boolean exitedWithError(DockerClient dockerClient, String containerId) {
            return !DockerStatus.isContainerExitCodeSuccess(getCurrentState(dockerClient, containerId));
        }
    }
}
