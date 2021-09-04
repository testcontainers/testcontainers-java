package org.testcontainers.providers.kubernetes;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeAddress;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jetbrains.annotations.Nullable;
import org.testcontainers.controller.intents.ExecCreateResult;
import org.testcontainers.providers.kubernetes.execution.KubernetesExecution;
import org.testcontainers.providers.kubernetes.intents.ExecCreateK8sIntent;
import org.testcontainers.providers.kubernetes.intents.ExecCreateK8sResult;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

public class KubernetesContext {

    private final KubernetesClient client;
    private final NamespaceProvider namespaceProvider;
    private final Map<String, ExecCreateK8sIntent> execCreateK8sIntents = new HashMap<>();
    private final Map<String, KubernetesExecution> execWatches = new HashMap<String, KubernetesExecution>();
    private final ExecutionLimiter executionLimiter = FixedExecutionLimiter.defaultLimiter();

    private final String sessionId = UUID.randomUUID().toString();

    private Optional<String> fixedNodePortAddress = Optional.empty();
    private KubernetesResourceReaper resourceReaper;

    public KubernetesContext(
        KubernetesClient client,
        NamespaceProvider namespaceProvider
    ) {
        this.client = client;
        this.namespaceProvider = namespaceProvider;
    }

    public KubernetesClient getClient() {
        return client;
    }

    public NamespaceProvider getNamespaceProvider() {
        return namespaceProvider;
    }

    private boolean isReady(Node node) {
        return node.getStatus().getConditions().stream().anyMatch(c ->
            "Ready".equals(c.getType()) && "True".equals(c.getStatus())
        );
    }

    public KubernetesContext withNodePortAddress(@Nullable String nodePortAddress) {
        this.fixedNodePortAddress = Optional.ofNullable(nodePortAddress);
        return this;
    }

    public Optional<String> getNodePortAddress() {
        if(fixedNodePortAddress.isPresent()) {
            return fixedNodePortAddress;
        }
        return detectNodePortAddress();
    }

    private Optional<String> detectNodePortAddress() {
        return client.nodes().list().getItems().stream()
            .filter(this::isReady)
            .map(this::getAddressOfNode)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findAny();
    }

    private Optional<String> getAddressOfNode(Node node) {
        return node.getStatus().getAddresses().stream()
            .filter(ip ->
                "InternalIP".equals(ip.getType()) || // TODO: Introduce strategy
                    "ExternalIP".equals(ip.getType())
            )
            .map(NodeAddress::getAddress)
            .findAny();
    }

    public Optional<Service> findServiceForReplicaSet(ReplicaSet replicaSet) {
        return Optional.ofNullable(
            client
                .services()
                .inNamespace(replicaSet.getMetadata().getNamespace())
                .withName(replicaSet.getMetadata().getName())
                .get()
        );
    }

    public ExecCreateResult registerCommand(ExecCreateK8sIntent execCreateK8sIntent) {
        String uuid = UUID.randomUUID().toString();
        execCreateK8sIntents.put(uuid, execCreateK8sIntent);
        return new ExecCreateK8sResult(uuid, execCreateK8sIntent);
    }

    public ExecCreateK8sIntent getCommand(String commandId) {
        return execCreateK8sIntents.get(commandId);
    }

    public ReplicaSet findReplicaSet(String containerId) {
        ReplicaSetList list = client
            .apps()
            .replicaSets()
            .inNamespace(namespaceProvider.getNamespace())
            .list();
        ReplicaSet replicaSet = list.getItems().stream()
            .filter(rs -> rs.getMetadata().getUid().equals(containerId))
            .findFirst()
            .get();
        return replicaSet;
    }

    public Pod findPodForContainerId(String containerId) {
        ReplicaSet replicaSet = findReplicaSet(containerId);
        return findPodForReplicaSet(replicaSet);
    }

    public Pod findPodForReplicaSet(ReplicaSet replicaSet) {
        Optional<Pod> foundPod;
        try {
            for (int i = 0; i < 5; i++) {
                foundPod = client
                    .pods()
                    .inNamespace(replicaSet.getMetadata().getNamespace())
                    .withLabelSelector(replicaSet.getSpec().getSelector())
                    .list()
                    .getItems()
                    .stream()
                    .findAny();
                if (foundPod.isPresent()) {
                    return foundPod.get();
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {

        }
        throw new NoSuchElementException("Pod could not be found.");
    }

    public void registerCommandWatch(String commandId, KubernetesExecution kubernetesExecution) {
        this.execWatches.put(commandId, kubernetesExecution);
    }

    public KubernetesExecution getCommandWatch(String commandId) {
        return this.execWatches.get(commandId);
    }

    public ExecutionLimiter getExecutionLimiter() {
        return executionLimiter;
    }

    public String getSessionId() {
        return sessionId;
    }

    public KubernetesContext withResourceReaper(KubernetesResourceReaper resourceReaper) {
        this.resourceReaper = resourceReaper;
        return this;
    }

    public KubernetesResourceReaper getResourceReaper() {
        return resourceReaper;
    }

}
