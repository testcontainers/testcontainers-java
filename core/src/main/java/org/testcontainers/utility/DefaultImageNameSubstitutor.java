package org.testcontainers.utility;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

/**
 * Testcontainers' default implementation of {@link ImageNameSubstitutor}.
 * Delegates to {@link ConfigurationFileImageNameSubstitutor}.
 */
@Slf4j
final class DefaultImageNameSubstitutor extends ImageNameSubstitutor {

    private final ConfigurationFileImageNameSubstitutor configurationFileImageNameSubstitutor;

    public DefaultImageNameSubstitutor() {
        configurationFileImageNameSubstitutor = new ConfigurationFileImageNameSubstitutor();
    }

    @VisibleForTesting
    DefaultImageNameSubstitutor(
        final ConfigurationFileImageNameSubstitutor configurationFileImageNameSubstitutor
    ) {
        this.configurationFileImageNameSubstitutor = configurationFileImageNameSubstitutor;
    }

    @Override
    public DockerImageName apply(final DockerImageName original) {
        return configurationFileImageNameSubstitutor.apply(original);
    }

    @Override
    protected String getDescription() {
        return "DefaultImageNameSubstitutor (" + configurationFileImageNameSubstitutor.getDescription() + ")";
    }
}
