package org.testcontainers.providers.kubernetes.model;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import org.testcontainers.controller.model.Binding;
import org.testcontainers.controller.model.ExposedPort;
import org.testcontainers.controller.model.Ports;

import java.util.HashMap;
import java.util.Map;

public class KubernetesPorts implements Ports {
    private final Service service;

    public KubernetesPorts(Service service) {
        this.service = service;
    }

    @Override
    public Map<ExposedPort, Binding[]> getBindings() {
        // TODO Lazy?
        // TODO NodePort?
        Map<ExposedPort, Binding[]> result = new HashMap<>();
        if(service == null) {
            return result;
        }
        for(ServicePort port : service.getSpec().getPorts()) {
            result.put(
                new KubernetesExposedPort(port),
                new Binding[]{ new KubernetesBinding(service, port) }
            );
        }
        return result;
    }

    @Override
    public Binding[] getBindings(int port) {
        return getBindings().entrySet().stream()
            .filter(e -> e.getKey().getPort() == port)
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(new Binding[0]);
    }
}
