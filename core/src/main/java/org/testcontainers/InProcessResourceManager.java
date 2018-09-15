package org.testcontainers;

import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.ListNetworksCmd;
import com.github.dockerjava.api.command.ListVolumesCmd;
import com.github.dockerjava.core.util.FiltersBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.dockerclient.DockerClientProviderStrategy;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Cleans allocated docker resources on JVM shutdown hook.
 */
final class InProcessResourceManager extends ResourceManagerBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(InProcessResourceManager.class);

    private final ConcurrentLinkedQueue<List<Map.Entry<String, String>>> registeredFilters = new ConcurrentLinkedQueue<>();

    InProcessResourceManager(DockerClientProviderStrategy strategy) {
        super(strategy.getClient());
    }

    @Override
    public synchronized void performCleanup() {
        super.performCleanup();

        Set<String> removedContainers = new HashSet<>();
        Set<String> removedNetworks = new HashSet<>();
        Set<String> removedVolumes = new HashSet<>();
        registeredFilters.forEach(
            entries -> removeResources(entries, removedContainers, removedNetworks, removedVolumes)
        );

        LOGGER.debug(
            "Removed {} container(s), {} network(s), {} volume(s)",
            removedContainers.size(),
            removedNetworks.size(),
            removedVolumes.size()
        );
    }

    @Override
    public void registerFilterForCleanup(List<Map.Entry<String, String>> filters) {
        setHook();
        registeredFilters.add(filters);
    }

    @Override
    protected Runnable getCleanupMethod() {
        return this::performCleanup;
    }

    private void removeResources(List<Map.Entry<String, String>> filter,
                                 Set<String> removedContainers,
                                 Set<String> removedNetworks,
                                 Set<String> removedVolumes) {
        FiltersBuilder filtersBuilder = new FiltersBuilder();
        filter.forEach(entry -> filtersBuilder.withFilter(entry.getKey(), entry.getValue()));
        Map<String, List<String>> filters = filtersBuilder.build();

        LOGGER.debug("Deleting resources matching filters " + filters);
        removeContainers(filters, removedContainers);
        removeNetworks(filters, removedNetworks);
        removeVolumes(filters, removedVolumes);
    }

    private void removeContainers(Map<String, List<String>> filters, Set<String> removedContainers) {
        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
        filters.forEach(listContainersCmd::withFilter);
        listContainersCmd.exec().forEach(container -> {
            try {
                dockerClient.removeContainerCmd(container.getId())
                    .withRemoveVolumes(true)
                    .withForce(true)
                    .exec();
                LOGGER.debug("Removed container: {}", container.getId());
                removedContainers.add(container.getId());
            } catch (Exception e) {
                LOGGER.trace("Failed to delete container {}: {}", container.getId(), e.getMessage());
            }
        });
    }

    private void removeNetworks(Map<String, List<String>> filters, Set<String> removedNetworks) {
        ListNetworksCmd listNetworksCmd = dockerClient.listNetworksCmd();
        addFilters(listNetworksCmd.getFilters(), filters);
        listNetworksCmd.exec().forEach(network -> {
            try {
                dockerClient.removeNetworkCmd(network.getId()).exec();
                LOGGER.debug("Removed network: {}", network.getId());
                removedNetworks.add(network.getId());
            } catch (Exception e) {
                LOGGER.trace("Failed to delete network {}: {}", network.getId(), e.getMessage());
            }
        });
    }

    private void removeVolumes(Map<String, List<String>> filters, Set<String> removedVolumes) {
        ListVolumesCmd listVolumesCmd = dockerClient.listVolumesCmd();
        addFilters(listVolumesCmd.getFilters(), filters);
        listVolumesCmd.exec().getVolumes().forEach(volume -> {
            try {
                dockerClient.removeVolumeCmd(volume.getName());
                LOGGER.debug("Removed volume: {}", volume.getName());
                removedVolumes.add(volume.getName());
            } catch (Exception e) {
                LOGGER.trace("Failed to delete volume {}: {}", volume.getName(), e.getMessage());
            }
        });
    }

    private static void addFilters(Map<String, List<String>> original,
                                   Map<String, List<String>> required) {
        if (original != null) {
            original.putAll(required);
        }
    }
}
