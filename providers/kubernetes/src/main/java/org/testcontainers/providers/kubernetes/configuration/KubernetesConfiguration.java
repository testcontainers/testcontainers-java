package org.testcontainers.providers.kubernetes.configuration;

import org.testcontainers.controller.configuration.CombinedConfigurationSource;
import org.testcontainers.controller.configuration.ConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class KubernetesConfiguration {

    private static final String NAMESPACE = "PROVIDER_KUBERNETES_NAMESPACE";
    private static final String NAMESPACE_LABELS = "PROVIDER_KUBERNETES_NAMESPACE_LABELS";
    private static final String NAMESPACE_ANNOTATIONS = "PROVIDER_KUBERNETES_NAMESPACE_ANNOTATIONS";

    private static final String NODEPORT_ADDRESS = "PROVIDER_KUBERNETES_NODEPORT_ADDRESS";

    private static final String TEMP_REGISTRY_INGRESS_HOST = "PROVIDER_KUBERNETES_TEMP_REGISTRY_INGRESS_HOST";
    private static final String TEMP_REGISTRY_INGRESS_ANNOTATIONS = "PROVIDER_KUBERNETES_TEMP_REGISTRY_INGRESS_ANNOTATIONS";
    private static final String TEMP_REGISTRY_INGRESS_CERT = "PROVIDER_KUBERNETES_TEMP_REGISTRY_INGRESS_CERT";

    private final CombinedConfigurationSource configurationSource;

    public KubernetesConfiguration(
        ConfigurationSource configurationSource
    ) {
        this.configurationSource = new CombinedConfigurationSource().addSource(configurationSource);
    }

    public Optional<String> getNamespacePattern() {
        return configurationSource.getEnvVarOrProperty(NAMESPACE);
    }

    public Optional<Map<String, String>> getNamespaceLabels() {
        return configurationSource.getEnvVarOrProperty(NAMESPACE_LABELS)
            .map(this::parseMap);
    }

    public Optional<Map<String, String>> getNamespaceAnnotations() {
        return configurationSource.getEnvVarOrProperty(NAMESPACE_ANNOTATIONS)
            .map(this::parseMap);
    }


    public Optional<String> getNodePortAddress() {
        return configurationSource.getEnvVarOrProperty(NODEPORT_ADDRESS);
    }

    public Optional<String> getTemporaryRegistryIngressHost() {
        return configurationSource.getEnvVarOrProperty(TEMP_REGISTRY_INGRESS_HOST);
    }

    public Optional<Map<String, String>> getTemporaryRegistryIngressAnnotations() {
        return configurationSource.getEnvVarOrProperty(TEMP_REGISTRY_INGRESS_ANNOTATIONS)
            .map(this::parseMap);
    }

    public Optional<String> getTemporaryIngressCert() {
        return configurationSource.getEnvVarOrProperty(TEMP_REGISTRY_INGRESS_CERT);
    }

    public CombinedConfigurationSource getConfigurationSource() {
        return configurationSource;
    }

    private Map<String, String> parseMap(String v) {
        return Arrays.stream(v.split(","))
            .flatMap(l -> Arrays.stream(l.split("\n")))
            .map(String::trim)
            .map(this::parseKeyValuePair)
            .collect(
                Collectors.toMap(
                    pair -> pair[0],
                    pair -> pair.length == 1 ? "" : pair[1]
                )
            );
    }

    private String[] parseKeyValuePair(String v) {
        List<String> result = new ArrayList<>();
        parseKeyValuePair(result, v);
        return result.toArray(new String[0]);
    }

    private void parseKeyValuePair(List<String> result, String v) {
        StringBuilder sb = new StringBuilder();
        char c;
        Optional<Character> quoteChar = Optional.empty();
        for (int i = 0; i < v.length(); i++) {
            c = v.charAt(i);
            if (i == 0 && (c == '\'' || c == '"')) {
                quoteChar = Optional.of(c);
                continue;
            }
            if (quoteChar.isPresent() && quoteChar.get() == c) {
                quoteChar = Optional.empty();
                continue;
            }
            if (!quoteChar.isPresent() && (c == ':' || c == '=')) {
                result.add(sb.toString());
                parseKeyValuePair(result, v.substring(i + 1));
                return;
            }
            sb.append(c);
        }
        result.add(sb.toString());
    }

}
