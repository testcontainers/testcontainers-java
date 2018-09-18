package org.testcontainers.utility;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.ListNetworksCmd;
import com.github.dockerjava.api.command.ListVolumesCmd;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides helper methods for docker client.
 */
public class DockerClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerClientUtil.class);

    /**
     * Removes docker resources matching specified filter.
     *
     * @param dockerClient is docker client.
     * @param filters is a filter.
     * @param removedContainers is a set to store ids of removed containers.
     * @param removedNetworks is a set to store ids of removed networks.
     * @param removedVolumes is a set to store names of removed volumes.
     */
    public static void removeResources(@NotNull DockerClient dockerClient,
                                @NotNull Map<String, List<String>> filters,
                                @NotNull Set<String> removedContainers,
                                @NotNull Set<String> removedNetworks,
                                @NotNull Set<String> removedVolumes) {
        removeContainers(dockerClient, filters, removedContainers);
        removeNetworks(dockerClient, filters, removedNetworks);
        removeVolumes(dockerClient, filters, removedVolumes);
    }

    private static void removeContainers(DockerClient dockerClient,
                                         Map<String, List<String>> filters,
                                         Set<String> removedContainers) {
        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd().withShowAll(true);
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

    private static void removeNetworks(DockerClient dockerClient,
                                       Map<String, List<String>> filters,
                                       Set<String> removedNetworks) {
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

    private static void removeVolumes(DockerClient dockerClient,
                                      Map<String, List<String>> filters,
                                      Set<String> removedVolumes) {
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
