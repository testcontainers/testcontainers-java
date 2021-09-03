package org.testcontainers.providers.kubernetes.configuration;

import org.testcontainers.controller.configuration.ConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class KubernetesConfiguration {

    private static final String PROVIDER_KUBERNETES_NAMESPACE = "PROVIDER_KUBERNETES_NAMESPACE";
    private static final String PROVIDER_KUBERNETES_NAMESPACE_LABELS = "PROVIDER_KUBERNETES_NAMESPACE_LABELS";

    private final ConfigurationSource configurationSource;

    public KubernetesConfiguration(
        ConfigurationSource configurationSource
    ) {
        this.configurationSource = configurationSource;
    }

    public Optional<String> getNamespacePattern() {
        return Optional.ofNullable(configurationSource.getEnvVarOrProperty(PROVIDER_KUBERNETES_NAMESPACE, null));
    }

    public Optional<Map<String, String>> getNamespaceLabels() {
        return Optional.ofNullable(configurationSource.getEnvVarOrProperty(PROVIDER_KUBERNETES_NAMESPACE_LABELS, null))
            .map(this::parseMap);
    }

    public Optional<Map<String, String>> getNamespaceAnnotations() {
        return Optional.ofNullable(configurationSource.getEnvVarOrProperty("PROVIDER_KUBERNETES_NAMESPACE_ANNOTATIONS", null))
            .map(this::parseMap);
    }



    private Map<String, String> parseMap(String v) {
        return Arrays.stream(v.split(","))
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
        for(int i=0; i<v.length(); i++) {
            c = v.charAt(i);
            if(i == 0 && (c == '\'' || c == '"')) {
                quoteChar = Optional.of(c);
                continue;
            }
            if(quoteChar.isPresent() && quoteChar.get() == c) {
                quoteChar = Optional.empty();
                continue;
            }
            if(!quoteChar.isPresent() && (c == ':' || c == '=')) {
                result.add(sb.toString());
                parseKeyValuePair(result, v.substring(i+1));
                return;
            }
            sb.append(c);
        }
        result.add(sb.toString());
    }

}
