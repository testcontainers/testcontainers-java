package org.testcontainers.providers.kubernetes.model;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import org.testcontainers.controller.model.Binding;

public class KubernetesBinding implements Binding {
    private final Service service;
    private final ServicePort port;

    public KubernetesBinding(Service service, ServicePort port) {
        this.service = service;
        this.port = port;
    }


    @Override
    public Integer getHostPort() {
        return port.getNodePort();
    }
}
