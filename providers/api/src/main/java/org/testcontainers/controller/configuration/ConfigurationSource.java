package org.testcontainers.controller.configuration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface ConfigurationSource {

    String getEnvVarOrProperty(@NotNull final String propertyName, @Nullable final String defaultValue);

    Optional<String> getEnvVarOrProperty(@NotNull final String propertyName);

}
