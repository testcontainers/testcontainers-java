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

    PumbaContainer() {
        super(PUMBA_DOCKER_IMAGE);
    }

    @Override
    public void execute(PumbaCommand command) {
        doNotWaitForStartupAtAll();
        mountDockerSocket();
        fetchIPRouteImage();

        executeCommand(command);
    }

    private void doNotWaitForStartupAtAll() {
        setStartupCheckStrategy(new DoNotCheckStartup());
    }

    private void mountDockerSocket() {
        addFileSystemBind("/var/run/docker.sock", "/var/run/docker.sock", READ_WRITE);
    }

    @SneakyThrows
    private void fetchIPRouteImage() {
        new RemoteDockerImage(IP_ROUTE_DOCKER_IMAGE).get();
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
