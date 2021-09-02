package org.testcontainers.providers.kubernetes.networking;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import org.testcontainers.providers.kubernetes.KubernetesContext;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NodePortStrategy implements NetworkStrategy {

    private static final Pattern SERVICE_NAME_PATTERN = Pattern.compile("^[a-z]([-a-z0-9]*[a-z0-9])?$");

    @Override
    public void apply(KubernetesContext ctx, NetworkingInitParameters parameters) {
        Container container = parameters.getContainer();
        Map<String, String> serviceSelector = getSelector(parameters);
        if(container.getPorts() != null && !container.getPorts().isEmpty()) {
            ServiceBuilder serviceBuilder = new ServiceBuilder();
            // @formatter:off
            serviceBuilder
                .editOrNewMetadata()
                    .withName(parameters.getWorkloadIdentifier())
                    .withNamespace(parameters.getNamespace())
                    .withLabels(serviceSelector)
                .endMetadata()
                .editOrNewSpec()
                    .withType("NodePort")
                    .withSelector(parameters.getLabels())
                .endSpec();
            // @formatter:on
            for (ContainerPort containerPort : container.getPorts()) {
                // @formatter:off
                serviceBuilder.editOrNewSpec()
                    .addNewPort()
                        .withName(String.format("%s-%d", containerPort.getProtocol().toLowerCase(), containerPort.getContainerPort()))
                        .withProtocol(containerPort.getProtocol())
                        .withTargetPort(new IntOrString(containerPort.getContainerPort()))
                        .withPort(containerPort.getContainerPort())
                    .endPort()
                    .endSpec();
                // @formatter:on
            }
            ctx.getClient().services().create(serviceBuilder.build());
        }

        for(String alias : getNormalizedValidAliases(parameters)) {
            ServiceBuilder serviceBuilder = new ServiceBuilder();
            // @formatter:off
            serviceBuilder
                .editOrNewMetadata()
                    .withName(alias)
                    .withNamespace(parameters.getNamespace())
                    .withLabels(serviceSelector)
                .endMetadata()
                .editOrNewSpec()
                    .withType("ClusterIP")
                    .withClusterIP("None")
                    .withSelector(parameters.getLabels())
                .endSpec();
            // @formatter:on
            ctx.getClient().services().createOrReplace(serviceBuilder.build());
        }

    }

    private Set<String> getNormalizedValidAliases(NetworkingInitParameters parameters) {
        return parameters.getNetworkAliases().stream()
            .map(String::toLowerCase)
            .filter(s -> SERVICE_NAME_PATTERN.matcher(s).matches())
            .collect(Collectors.toSet());
    }

    @Override
    public Service find(KubernetesContext ctx, String namespace, String identifier) {
        return ctx.getClient().services()
            .inNamespace(namespace)
            .withName(identifier)
            .get();
    }

    @Override
    public void teardown(KubernetesContext ctx, String namespace, String identifier) {
        Optional<Service> mainService = Optional.ofNullable(find(ctx, namespace, identifier));
        mainService.ifPresent(service -> ctx.getClient()
            .services()
            .inNamespace(namespace)
            .withLabels(service.getMetadata().getLabels())
            .delete());
    }

    private Map<String, String> getSelector(NetworkingInitParameters parameters) {
        return parameters.getLabels();
    }

}
