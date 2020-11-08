package org.testcontainers.utility;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

/**
 * Testcontainers' default implementation of {@link ImageNameSubstitutor}.
 * Delegates to {@link ConfigurationFileImageNameSubstitutor} followed by {@link PrefixingImageNameSubstitutor}.
 * <p>
 * WARNING: this class is not intended to be public, but {@link java.util.ServiceLoader}
 * requires it to be so. Public visibility DOES NOT make it part of the public API.
 */
@Slf4j
public class DefaultImageNameSubstitutor extends ImageNameSubstitutor {

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
