package org.testcontainers.providers.kubernetes;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.SneakyThrows;
import org.testcontainers.controller.ContainerController;
import org.testcontainers.controller.ContainerProvider;

public class KubernetesContainerProvider implements ContainerProvider {

    @Override
    public ContainerController lazyController() {
        return controller(); // TODO: Implement actual lazy controller
    }

    @Override
    @SneakyThrows
    public ContainerController controller() {

        return new KubernetesContainerController(
            kubernetesContext()
        );
    }

    private KubernetesContext kubernetesContext() {
        KubernetesClient client = new DefaultKubernetesClient();
        NamespaceProvider namespaceProvider = new StaticNamespaceProvider("testcontainers");
        return new KubernetesContext(client, namespaceProvider);
    }

    @Override
    public String exposedPortsIpAddress() {
        return kubernetesContext().getNodePortAddress().get();
    }

    @Override
    public boolean supportsExecution() {
        return true;
    }
}
