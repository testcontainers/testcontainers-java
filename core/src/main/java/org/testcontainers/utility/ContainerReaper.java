package org.testcontainers.utility;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Component that responsible for container removal and automatic cleanup of dead containers at JVM shutdown.
 */
public final class ContainerReaper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerReaper.class);
    private static ContainerReaper instance;
    private final DockerClient dockerClient;
    private Map<String, String> registeredContainers = new ConcurrentHashMap<>();

    private ContainerReaper() {
        dockerClient = DockerClientFactory.instance().client();

        // If the JVM stops without containers being stopped, try and stop the container.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            registeredContainers.forEach(this::stopContainer);
        }));
    }

    public synchronized static ContainerReaper instance() {
        if (instance == null) {
            instance = new ContainerReaper();
        }

        return instance;
    }

    /**
     * Register a container to be cleaned up, either on explicit call to stopAndRemoveContainer, or at JVM shutdown.
     * @param containerId the ID of the container
     * @param imageName the image name of the container (used for logging)
     */
    public void registerContainerForCleanup(String containerId, String imageName) {
        registeredContainers.put(containerId, imageName);
    }

    /**
     * Stop a potentially running container and remove it, including associated volumes.
     * @param containerId the ID of the container
     */
    public void stopAndRemoveContainer(String containerId) {
        stopContainer(containerId, registeredContainers.get(containerId));
    }

    /**
     * Stop a potentially running container and remove it, including associated volumes.
     * @param containerId the ID of the container
     * @param imageName the image name of the container (used for logging)
     */
    public void stopAndRemoveContainer(String containerId, String imageName) {
        stopContainer(containerId, imageName);

        registeredContainers.remove(containerId);
    }

    private void stopContainer(String containerId, String imageName) {
        try {
            LOGGER.trace("Stopping container: {}", containerId);
            dockerClient.killContainerCmd(containerId).exec();
            LOGGER.trace("Stopped container: {}", imageName);
        } catch (DockerException e) {
            LOGGER.trace("Error encountered shutting down container (ID: {}) - it may not have been stopped, or may already be stopped: {}", containerId, e.getMessage());
        }

        try {
            LOGGER.trace("Stopping container: {}", containerId);
            try {
                dockerClient.removeContainerCmd(containerId).withRemoveVolumes(true).withForce(true).exec();
                LOGGER.info("Removed container and associated volume(s): {}", imageName);
            } catch (InternalServerErrorException e) {
                LOGGER.trace("Exception when removing container with associated volume(s): {} (due to {})", imageName, e.getMessage());
            }
        } catch (DockerException e) {
            LOGGER.trace("Error encountered shutting down container (ID: {}) - it may not have been stopped, or may already be stopped: {}", containerId, e.getMessage());
        }
    }
}