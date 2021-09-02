package org.testcontainers.providers.kubernetes.networking;

import io.fabric8.kubernetes.api.model.Container;
import lombok.Getter;
import lombok.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Getter
public class NetworkingInitParameters {

    private final String namespace;
    private final String workloadIdentifier;
    private final Map<String, String> labels;
    private final Container container;
    @NonNull
    private final Set<String> networkAliases;

    public NetworkingInitParameters(
        String namespace,
        String identifier,
        Map<String, String> labels,
        Container container,
        Set<String> networkAliases) {
        this.namespace = namespace;
        this.workloadIdentifier = identifier;
        this.labels = labels;
        this.container = container;
        this.networkAliases = Optional.ofNullable(networkAliases).orElseGet(Collections::emptySet);
    }
}
