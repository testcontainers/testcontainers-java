package org.testcontainers.utility;

import com.github.dockerjava.api.DockerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.ResourceManager;

import java.util.List;
import java.util.Map;

/**
 * Component that responsible for container removal and automatic cleanup of dead containers at JVM shutdown.
 *
 * @deprecated Use {@code DockerClientFactory.instance().getResourceManager()} instead.
 */
@Deprecated
@SuppressWarnings("unused")
public class ResourceReaper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceReaper.class);

    private static ResourceReaper instance;
    private final ResourceManager resourceManager;

    private ResourceReaper() {
        resourceManager = DockerClientFactory.instance().getResourceManager();
    }

    public static String start(String hostIpAddress, DockerClient client) {
        return start(hostIpAddress, client, false);
    }

    public static String start(String hostIpAddress, DockerClient client, boolean withDummyMount) {
        return null;
    }

    public synchronized static ResourceReaper instance() {
        if (instance == null) {
            instance = new ResourceReaper();
        }

        return instance;
    }

    /**
     * Perform a cleanup.
     */
    public void performCleanup() {
        resourceManager.performCleanup();
    }

    /**
     * Register a filter to be cleaned up.
     *
     * @param filter the filter
     */
    public void registerFilterForCleanup(List<Map.Entry<String, String>> filter) {
        resourceManager.registerFilterForCleanup(filter);
    }

    /**
     * Register a container to be cleaned up, either on explicit call to stopAndRemoveContainer, or at JVM shutdown.
     *
     * @param containerId the ID of the container
     * @param imageName   the image name of the container (used for logging)
     */
    public void registerContainerForCleanup(String containerId, String imageName) {
        resourceManager.registerContainerForCleanup(containerId, imageName);
    }

    /**
     * Stop a potentially running container and remove it, including associated volumes.
     *
     * @param containerId the ID of the container
     */
    public void stopAndRemoveContainer(String containerId) {
        resourceManager.stopAndRemoveContainer(containerId);
    }

    /**
     * Stop a potentially running container and remove it, including associated volumes.
     *
     * @param containerId the ID of the container
     * @param imageName   the image name of the container (used for logging)
     */
    public void stopAndRemoveContainer(String containerId, String imageName) {
        resourceManager.stopAndRemoveContainer(containerId, imageName);
    }

    /**
     * Register a network to be cleaned up at JVM shutdown.
     *
     * @param id   the ID of the network
     */
    public void registerNetworkIdForCleanup(String id) {
        resourceManager.registerNetworkIdForCleanup(id);
    }

    /**
     * @param networkName   the name of the network
     * @deprecated see {@link ResourceReaper#registerNetworkIdForCleanup(String)}
     */
    @Deprecated
    public void registerNetworkForCleanup(String networkName) {
        try {
            // Try to find the network by name, so that we can register its ID for later deletion
            DockerClientFactory.instance().client().listNetworksCmd()
                .withNameFilter(networkName)
                .exec()
                .forEach(network -> resourceManager.registerNetworkIdForCleanup(network.getId()));
        } catch (Exception e) {
            LOGGER.trace("Error encountered when looking up network (name: {})", networkName);
        }
    }

    /**
     * Removes a network by ID.
     * @param id
     */
    public void removeNetworkById(String id) {
        resourceManager.removeNetworkById(id);
    }

    /**
     * Removes a network by ID.
     * @param identifier
     * @deprecated see {@link ResourceReaper#removeNetworkById(String)}
     */
    @Deprecated
    public void removeNetworks(String identifier) {
        removeNetworkById(identifier);
    }

    public void unregisterNetwork(String identifier) {
        resourceManager.unregisterNetwork(identifier);
    }

    public void unregisterContainer(String identifier) {
        resourceManager.unregisterContainer(identifier);
    }
}
