package org.testcontainers.utility;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.PruneType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A {@link ResourceReaper} implementation that uses {@link Runtime#addShutdownHook(Thread)}
 * to cleanup containers.
 */
class JVMHookResourceReaper extends ResourceReaper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JVMHookResourceReaper.class);
    private static final int MAX_CONTAINER_REMOVAL_ATTEMPTS = 5;

    @Override
    public void init() {
        setHook();
    }

    /**
     * Perform a cleanup.
     * @deprecated no longer supported API, use {@link DockerClient} directly
     */
    @Override
    @Deprecated
    public synchronized void performCleanup() {
        super.performCleanup();
        synchronized (DEATH_NOTE) {
            tryPrune(PruneType.CONTAINERS);
            tryPrune(PruneType.NETWORKS);
            tryPrune(PruneType.VOLUMES);
            tryPrune(PruneType.IMAGES);
        }
    }

    /**
     * For each of the label filter sets registered in
     * {@link ResourceReaper#DEATH_NOTE}, attempt to prune resources of type
     * pruneType.
     * 
     * @param pruneType the resource type being pruned
     */
    private void tryPrune(PruneType pruneType) {
        DEATH_NOTE.forEach(filters -> {
            try {
                prune(pruneType, filters);
            } catch (Exception e) {
                LOGGER.warn("Exception pruning {} resources with labels {}: {}", pruneType, filters, e.getMessage(), e);
            }
        });
    }

    /**
     * Prune resources of type pruneType that have the provided label filters. While
     * containers must first be listed prior to removal and failures will be
     * retried, all other resource types will simply use the Docker client's prune
     * command directly.
     * 
     * @param pruneType the resource type being pruned
     * @param filters   label filters that must be present on the resources being
     *                  pruned
     */
    private void prune(PruneType pruneType, List<Map.Entry<String, String>> filters) {
        String[] labels = filters
            .stream()
            .filter(it -> "label".equals(it.getKey()))
            .map(Map.Entry::getValue)
            .toArray(String[]::new);

        if (pruneType == PruneType.CONTAINERS) {
            // Docker only prunes stopped containers, so we have to do it manually
            removeContainers(labels);
        } else {
            dockerClient.pruneCmd(pruneType).withLabelFilter(labels).exec();
        }
    }

    /**
     * Find any remove any containers having the given labels. Up to 5 attempts to
     * forcibly remove the containers will be made. Any errors that occur will be
     * provided as suppressed exceptions of a thrown RuntimeException should they
     * occur.
     * 
     * @param labels container labels to find and remove
     */
    private void removeContainers(String[] labels) {
        List<Container> containers = listContainers(labels);
        int retries = MAX_CONTAINER_REMOVAL_ATTEMPTS;

        while (!containers.isEmpty() && retries-- > 0) {
            List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

            containers.parallelStream().forEach(container -> removeContainer(container, errors));

            if (errors.isEmpty()) {
                containers = Collections.emptyList();
            } else if (retries < 1) {
                RuntimeException removeError = new RuntimeException("Error removing one or more containers");
                errors.forEach(removeError::addSuppressed);
                throw removeError;
            } else {
                containers = listContainers(labels);
            }
        }
    }

    /**
     * List all containers with the given labels.
     * 
     * @param labels container labels to find
     */
    private List<Container> listContainers(String[] labels) {
        return dockerClient.listContainersCmd().withFilter("label", Arrays.asList(labels)).withShowAll(true).exec();
    }

    /**
     * Forcibly remove the given container. Any thrown exception will be added to
     * the list of errors.
     * 
     * @param container the container to stop
     * @param errors    list of errors to add any occurring exceptions
     */
    private void removeContainer(Container container, List<Throwable> errors) {
        try {
            dockerClient.removeContainerCmd(container.getId()).withForce(true).withRemoveVolumes(true).exec();
        } catch (Exception e) {
            errors.add(e);
        }
    }
}
