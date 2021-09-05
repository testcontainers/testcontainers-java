package org.testcontainers.providers.kubernetes.configuration;

import io.fabric8.kubernetes.api.model.ConfigMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testcontainers.controller.configuration.ConfigurationSource;

import java.util.Map;
import java.util.Optional;

public class ConfigMapConfigurationSource implements ConfigurationSource {

    private final ConfigMap configMap;

    public ConfigMapConfigurationSource(
        ConfigMap configMap
    ) {
        this.configMap = configMap;
    }


    @Override
    public String getEnvVarOrProperty(@NotNull String propertyName, @Nullable String defaultValue) {
        return getEnvVarOrProperty(propertyName).orElse(defaultValue);
    }

    @Override
    public Optional<String> getEnvVarOrProperty(@NotNull String propertyName) {
        Map<String, String> data = configMap.getData();
        if(data.containsKey(propertyName)) {
            return Optional.of(data.get(propertyName));
        }
        return Optional.empty();
    }
}
