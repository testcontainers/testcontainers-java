package org.testcontainers.providers.kubernetes;

import com.github.dockerjava.api.model.Ports;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import org.jetbrains.annotations.Nullable;
import org.testcontainers.controller.ContainerController;
import org.testcontainers.controller.ResourceCleaner;
import org.testcontainers.controller.intents.RemoveContainerIntent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class KubernetesResourceReaper implements ResourceCleaner {

    private final KubernetesContext ctx;
    private final ContainerController containerController;
    private AtomicBoolean hookIsSet = new AtomicBoolean(false);


    private final Set<String> containerIds = new HashSet<>();
    @Nullable
    private Namespace createdNamespace = null;
    private List<Deployment> deployments = new ArrayList<>();
    private List<Service> services = new ArrayList<>();
    private List<Ingress> ingresses = new ArrayList<>();

    public KubernetesResourceReaper(
        KubernetesContext ctx,
        ContainerController containerController
    ) {
        this.ctx = ctx.withResourceReaper(this);
        this.containerController = containerController;
    }

    @Override
    public void stopAndRemoveContainer(String containerId, String imageName) {
        containerController.removeContainerIntent(containerId).perform();
    }

    @Override
    public void registerFilterForCleanup(List<Map.Entry<String, String>> label) {

    }

    @Override
    public void removeNetworkById(String id) {

    }

    @Override
    public void registerImageForCleanup(String imageReference) {

    }

    @Override
    public String start() {
        return "empty"; // TODO: Implement!
    }

    @Override
    public void registerContainerForCleanup(String containerId) {
        setHook();
        containerIds.add(containerId);
    }

    private void performCleanup() {
        containerIds.stream().map(containerController::removeContainerIntent).forEach(RemoveContainerIntent::perform);
        ctx.getClient().apps().deployments().delete(deployments);
        ctx.getClient().services().delete(services);
        ctx.getClient().extensions().ingresses().delete(ingresses);
        if(this.createdNamespace != null) {
            ctx.getClient()
                .namespaces()
                .delete(createdNamespace);
        }
    }

    private void setHook() {
        if (hookIsSet.compareAndSet(false, true)) {
            // If the JVM stops without containers being stopped, try and stop the container.
            Runtime.getRuntime().addShutdownHook(new Thread(this::performCleanup));
        }
    }

    public void registerNamespaceForCleanup(Namespace createdNamespace) {
        setHook();
        this.createdNamespace = createdNamespace;
    }

    public void registerDeploymentForCleanup(Deployment createdDeployment) {
        setHook();
        this.deployments.add(createdDeployment);
    }

    public void registerServiceForCleanup(Service createdService) {
        setHook();
        this.services.add(createdService);
    }

    public void registerIngressForCleanup(Ingress createdIngress) {
        setHook();
        this.ingresses.add(createdIngress);
    }
}
