package org.testcontainers.controller.configuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DefaultConfigurationSource implements ConfigurationSource {

    @Override
    public String getEnvVarOrProperty(@NotNull String propertyName, @Nullable String defaultValue) {
        return defaultValue;
    }

    @Override
    public Optional<String> getEnvVarOrProperty(@NotNull String propertyName) {
        return Optional.empty();
    }
}
