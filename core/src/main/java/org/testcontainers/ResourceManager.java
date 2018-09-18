package org.testcontainers;

import java.util.List;
import java.util.Map;

/**
 * Component that responsible for container removal and automatic cleanup of dead containers at JVM shutdown.
 */
public interface ResourceManager {

    /**
     * Executes initialization process.
     */
    void initialize();

    /**
     * Perform a cleanup.
     */
    void performCleanup();

    /**
     * Register a filter to be cleaned up.
     *
     * @param filter the filter
     */
    void registerFilterForCleanup(List<Map.Entry<String, String>> filter);

    /**
     * Register a container to be cleaned up, either on explicit call to stopAndRemoveContainer, or at JVM shutdown.
     *
     * @param containerId the ID of the container
     * @param imageName   the image name of the container (used for logging)
     */
    void registerContainerForCleanup(String containerId, String imageName);

    /**
     * Stop a potentially running container and remove it, including associated volumes.
     *
     * @param containerId the ID of the container
     */
    void stopAndRemoveContainer(String containerId);

    /**
     * Stop a potentially running container and remove it, including associated volumes.
     *
     * @param containerId the ID of the container
     * @param imageName   the image name of the container (used for logging)
     */
    void stopAndRemoveContainer(String containerId, String imageName);

    /**
     * Register a network to be cleaned up at JVM shutdown.
     *
     * @param id   the ID of the network
     */
    void registerNetworkIdForCleanup(String id);

    /**
     * Removes a network by ID.
     *
     * @param id the network identifier.
     */
    void removeNetworkById(String id);

    void unregisterContainer(String identifier);

    void unregisterNetwork(String identifier);
}
