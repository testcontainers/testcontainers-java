package org.testcontainers.utility;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.PruneType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A {@link ResourceReaper} implementation that uses {@link Runtime#addShutdownHook(Thread)}
 * to cleanup containers.
 */
class JVMHookResourceReaper extends ResourceReaper {

    @Override
    public void init() {
        setHook();
    }

    @Override
    public synchronized void performCleanup() {
        super.performCleanup();
        synchronized (DEATH_NOTE) {
            DEATH_NOTE.forEach(filters -> prune(PruneType.CONTAINERS, filters));
            DEATH_NOTE.forEach(filters -> prune(PruneType.NETWORKS, filters));
            DEATH_NOTE.forEach(filters -> prune(PruneType.VOLUMES, filters));
            DEATH_NOTE.forEach(filters -> prune(PruneType.IMAGES, filters));
        }
    }

    private void prune(PruneType pruneType, List<Map.Entry<String, String>> filters) {
        String[] labels = filters
            .stream()
            .filter(it -> "label".equals(it.getKey()))
            .map(Map.Entry::getValue)
            .toArray(String[]::new);
        switch (pruneType) {
            // Docker only prunes stopped containers, so we have to do it manually
            case CONTAINERS:
                List<Container> containers = dockerClient
                    .listContainersCmd()
                    .withFilter("label", Arrays.asList(labels))
                    .withShowAll(true)
                    .exec();

                containers
                    .parallelStream()
                    .forEach(container -> {
                        dockerClient
                            .removeContainerCmd(container.getId())
                            .withForce(true)
                            .withRemoveVolumes(true)
                            .exec();
                    });
                break;
            default:
                dockerClient.pruneCmd(pruneType).withLabelFilter(labels).exec();
                break;
        }
    }
}
