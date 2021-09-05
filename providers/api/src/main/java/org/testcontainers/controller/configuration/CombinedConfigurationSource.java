package org.testcontainers.controller.configuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CombinedConfigurationSource implements ConfigurationSource {

    private final List<ConfigurationSource> sources;

    public CombinedConfigurationSource(
        List<ConfigurationSource> sources
    ) {
        this.sources = sources;
    }

    public CombinedConfigurationSource() {
        this(new ArrayList<>());
    }

    @Override
    public String getEnvVarOrProperty(@NotNull String propertyName, @Nullable String defaultValue) {
        return getEnvVarOrProperty(propertyName).orElse(defaultValue);
    }

    @Override
    public Optional<String> getEnvVarOrProperty(@NotNull String propertyName) {
        return sources.stream()
            .map(src -> src.getEnvVarOrProperty(propertyName))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    public CombinedConfigurationSource addSource(ConfigurationSource source) {
        sources.add(source);
        return this;
    }

}
