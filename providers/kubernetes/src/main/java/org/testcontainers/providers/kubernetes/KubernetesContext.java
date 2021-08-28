package org.testcontainers.providers.kubernetes;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeAddress;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.testcontainers.controller.intents.ExecCreateResult;
import org.testcontainers.providers.kubernetes.execution.KubernetesExecution;
import org.testcontainers.providers.kubernetes.intents.ExecCreateK8sIntent;
import org.testcontainers.providers.kubernetes.intents.ExecCreateK8sResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class KubernetesContext {

    private final KubernetesClient client;
    private final NamespaceProvider namespaceProvider;
    private final Map<String, ExecCreateK8sIntent> execCreateK8sIntents = new HashMap<>();
    private final Map<String, KubernetesExecution> execWatches = new HashMap<String, KubernetesExecution>();

    public KubernetesContext(KubernetesClient client, NamespaceProvider namespaceProvider) {
        this.client = client;
        this.namespaceProvider = namespaceProvider;
    }

    public KubernetesClient getClient() {
        return client;
    }

    public NamespaceProvider getNamespaceProvider() {
        return namespaceProvider;
    }


    private Optional<String> getNodeAddress(Node node) {
        return node.getStatus().getAddresses().stream()
            .filter(ip ->
                "InternalIP".equals(ip.getType()) || // TODO: Introduce strategy
                    "ExternalIP".equals(ip.getType())
            )
            .map(NodeAddress::getAddress)
            .findAny();
    }

    private boolean isReady(Node node) {
        return node.getStatus().getConditions().stream().anyMatch(c ->
            "Ready".equals(c.getType()) && "True".equals(c.getStatus())
        );
    }

    public Optional<String> getNodePortAddress() {
        return client.nodes().list().getItems().stream()
            .filter(this::isReady)
            .map(this::getNodeAddress)
            .filter(Optional::isPresent)
            .map(Optional::get)
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
        return client
            .pods()
            .inNamespace(replicaSet.getMetadata().getNamespace())
            .withLabelSelector(replicaSet.getSpec().getSelector())
            .list()
            .getItems()
            .stream()
            .findAny()
            .get();
    }

    public void registerCommandWatch(String commandId, KubernetesExecution kubernetesExecution) {
        this.execWatches.put(commandId, kubernetesExecution);
    }

    public KubernetesExecution getCommandWatch(String commandId) {
        return this.execWatches.get(commandId);
    }
}
