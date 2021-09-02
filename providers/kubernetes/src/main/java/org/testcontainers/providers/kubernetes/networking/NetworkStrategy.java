package org.testcontainers.providers.kubernetes.networking;

import io.fabric8.kubernetes.api.model.Service;
import org.testcontainers.providers.kubernetes.KubernetesContext;

public interface NetworkStrategy {

    void apply(KubernetesContext ctx, NetworkingInitParameters parameters);
    Service find(KubernetesContext ctx, String namespace, String identifier);

    void teardown(KubernetesContext ctx, String namespace, String identifier);
}
