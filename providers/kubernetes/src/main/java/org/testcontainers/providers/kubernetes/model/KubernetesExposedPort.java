package org.testcontainers.providers.kubernetes.model;

import io.fabric8.kubernetes.api.model.ServicePort;
import org.testcontainers.controller.model.ExposedPort;

public class KubernetesExposedPort implements ExposedPort {
    private final ServicePort port;

    public KubernetesExposedPort(ServicePort port) {
        this.port = port;
    }

    @Override
    public int getPort() {
        return port.getTargetPort().getIntVal();
    }
}
