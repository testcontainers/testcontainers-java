package org.testcontainers.providers.kubernetes;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.controller.ContainerController;
import org.testcontainers.controller.ContainerProvider;

import java.util.UUID;

@Slf4j
public class KubernetesContainerProvider implements ContainerProvider {

    private static final String PROVIDER_IDENTIFIER = "kubernetes";

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

    @Override
    public boolean isFileMountingSupported() {
        return true;
    }

    @Override
    public String getRandomImageName() {
        return "docker.cluster.lise.de/testcontainers/" + UUID.randomUUID().toString().toLowerCase(); // TODO: Implement temp registry
    }

    @Override
    public String getIdentifier() {
        return PROVIDER_IDENTIFIER;
    }

    @Override
    public boolean isAvailable() {
        try {
            KubernetesClient kc = new DefaultKubernetesClient();
            return kc.getVersion() != null;
        }catch (Throwable th) {
            log.debug("Kubernetes is not available. See stack trace for more details.", th);
            return false;
        }
    }
}
