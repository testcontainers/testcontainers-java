package org.testcontainers;

import com.github.dockerjava.api.DockerClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.client.PumbaCommand;
import org.testcontainers.client.PumbaExecutable;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.images.RemoteDockerImage;

import static org.testcontainers.containers.BindMode.READ_WRITE;

/**
 * Created by novy on 31.12.16.
 */
@Slf4j
class PumbaContainer extends GenericContainer<PumbaContainer> implements PumbaExecutable {

    private static final String PUMBA_DOCKER_IMAGE = "gaiaadm/pumba:0.4.7";
    private static final String IP_ROUTE_DOCKER_IMAGE = "gaiadocker/iproute2:3.3";

    private static final String DOCKER_SOCKET_HOST_PATH = "/var/run/docker.sock";
    private static final String DOCKER_SOCKET_CONTAINER_PATH = "/docker.sock";

    PumbaContainer() {
        super(PUMBA_DOCKER_IMAGE);
        doNotWaitForStartupAtAll();
        mountDockerSocket();
        fetchIPRouteImage();
        setupLogging();
    }

    @Override
    public void execute(PumbaCommand command) {
        executeCommand(command);
    }

    private void doNotWaitForStartupAtAll() {
        setStartupCheckStrategy(new DoNotCheckStartup());
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
        withLogConsumer(frame -> log.debug("Pumba container: \"{}\"", frame.getUtf8String()));
    }

    private void executeCommand(PumbaCommand command) {
        final String evaluatedCommand = command.evaluate();
        setCommand(evaluatedCommand);
        log.info("Executing pumba container with command \"{}\"", evaluatedCommand);
        super.start();
    }

    private static class DoNotCheckStartup extends StartupCheckStrategy {
        @Override
        public StartupStatus checkStartupState(DockerClient dockerClient, String s) {
            return StartupStatus.SUCCESSFUL;
        }
    }
}
