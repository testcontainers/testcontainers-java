package org.testcontainers.providers.kubernetes.model;

import com.github.dockerjava.api.command.HealthState;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.controller.model.ContainerState;
import org.testcontainers.providers.kubernetes.KubernetesContext;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

// TODO: Check properties
@Slf4j
public class KubernetesContainerState implements ContainerState {
    private final KubernetesContext ctx;
    private final ReplicaSet replicaSet;
    private final String containerId;

    public KubernetesContainerState(KubernetesContext ctx, ReplicaSet replicaSet, String containerId) {
        this.ctx = ctx;
        this.replicaSet = replicaSet;
        this.containerId = containerId;
    }



    private Optional<Pod> getPod() {
        return Optional.of(ctx.findPodForContainerId(containerId));
    }

    private Stream<ContainerStatus> getContainerStatuses() {
        return getPod().map(p -> p.getStatus().getContainerStatuses()).map(Collection::stream).orElse(Stream.empty());
    }

    @Override
    public String getStatus() {
        return getPod().map(p -> p.getStatus().getPhase()).orElse(null);
    }

    @Override
    public Boolean getRunning() {
        return getPod().map(p -> "Running".equals(p.getStatus().getPhase())).orElse(false);
    }

    @Override
    public Integer getExitCode() {
        return getContainerStatuses()
            .map(ContainerStatus::getLastState)
            .filter(Objects::nonNull)
            .map(io.fabric8.kubernetes.api.model.ContainerState::getTerminated)
            .filter(Objects::nonNull)
            .map(ContainerStateTerminated::getExitCode)
            .findFirst()
            .orElse(0); // Return zero anyway to indicate the container is still healthy See DockerStatus.isContainerExitCodeSuccess(ContainerState state)  // TODO: Really
    }

    @Override
    public Boolean getDead() {
        return false; // TODO Implement!
    }

    @Override
    public Boolean getOOMKilled() {
        return false; // TODO Implement!
    }

    @Override
    public String getError() {
        return getPod().map(pod -> pod.getStatus().getReason()).orElse(null);
    }

    @Override
    public HealthState getHealth() {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public boolean getPaused() {
        return replicaSet.getSpec().getReplicas() == 0;
    }

    @Override
    public String getStartedAt() {
        return getPod()
            .map(Pod::getStatus)
            .map(PodStatus::getStartTime)
            .orElse(null); // TODO: Or else?
    }

    @Override
    public String getFinishedAt() {
        return null; // TODO: Implement
    }
}
