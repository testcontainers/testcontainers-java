package org.testcontainers.utility;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

/**
 * Testcontainers' default implementation of {@link ImageNameSubstitutor}.
 * Delegates to {@link ConfigurationFileImageNameSubstitutor}.
 * <p>
 * WARNING: this class is not intended to be public, but {@link java.util.ServiceLoader}
 * requires it to be so. Public visibility DOES NOT make it part of the public API.
 */
@Slf4j
public class DefaultImageNameSubstitutor extends ImageNameSubstitutor {

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
    protected int getPriority() {
        return 0;
    }

    @Override
    protected String getDescription() {
        return "DefaultImageNameSubstitutor (delegates to '" + configurationFileImageNameSubstitutor.getDescription() + "')";
    }
}
