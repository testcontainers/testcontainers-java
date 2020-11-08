package org.testcontainers.utility;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

/**
 * Testcontainers' default implementation of {@link ImageNameSubstitutor}.
 * Delegates to {@link ConfigurationFileImageNameSubstitutor} followed by {@link PrefixingImageNameSubstitutor}.
 */
@Slf4j
final class DefaultImageNameSubstitutor extends ImageNameSubstitutor {

    private final ConfigurationFileImageNameSubstitutor configurationFileImageNameSubstitutor;
    private final PrefixingImageNameSubstitutor prefixingImageNameSubstitutor;

    public DefaultImageNameSubstitutor() {
        configurationFileImageNameSubstitutor = new ConfigurationFileImageNameSubstitutor();
        prefixingImageNameSubstitutor = new PrefixingImageNameSubstitutor();
    }

    @VisibleForTesting
    DefaultImageNameSubstitutor(
        final ConfigurationFileImageNameSubstitutor configurationFileImageNameSubstitutor,
        final PrefixingImageNameSubstitutor prefixingImageNameSubstitutor
    ) {
        this.configurationFileImageNameSubstitutor = configurationFileImageNameSubstitutor;
        this.prefixingImageNameSubstitutor = prefixingImageNameSubstitutor;
    }

    @Override
    public DockerImageName apply(final DockerImageName original) {
        return configurationFileImageNameSubstitutor
            .andThen(prefixingImageNameSubstitutor)
            .apply(original);
    }

    @Override
    protected String getDescription() {
        return "DefaultImageNameSubstitutor (composite of '" + configurationFileImageNameSubstitutor.getDescription() + "' and '" + prefixingImageNameSubstitutor.getDescription() + "')";
    }
}
