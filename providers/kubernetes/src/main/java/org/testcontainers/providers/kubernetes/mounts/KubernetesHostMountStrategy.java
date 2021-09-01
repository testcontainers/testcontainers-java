package org.testcontainers.providers.kubernetes.mounts;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodTemplateSpecFluent;
import org.testcontainers.controller.model.HostMount;
import org.testcontainers.providers.kubernetes.KubernetesContext;

import java.util.List;

public interface KubernetesHostMountStrategy {


    KubernetesHostMountStrategy withHostMounts(List<HostMount> hostMounts);

    <T extends PodTemplateSpecFluent.SpecNested<?>> T configure(T templateNestedSpecNested);

    void apply(KubernetesContext ctx, Pod pod);
}
