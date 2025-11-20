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

    private void tryPrune(PruneType pruneType) {
        try {
            DEATH_NOTE.forEach(filters -> prune(pruneType, filters));
        } catch (Exception e) {
            LOGGER.warn("Exception pruning {} resources: {}", pruneType, e.getMessage(), e);
        }
    }

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

    private void removeContainers(String[] labels) {
        List<Container> containers = listContainers(labels);
        int retries = 5;

        while (!containers.isEmpty() && retries-- > 0) {
            List<Throwable> errors = new ArrayList<>();

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

    private List<Container> listContainers(String[] labels) {
        return dockerClient.listContainersCmd().withFilter("label", Arrays.asList(labels)).withShowAll(true).exec();
    }

    private void removeContainer(Container container, List<Throwable> errors) {
        try {
            dockerClient.removeContainerCmd(container.getId()).withForce(true).withRemoveVolumes(true).exec();
        } catch (Exception e) {
            errors.add(e);
        }
    }
}
