package org.testcontainers.utility;

import com.google.common.annotations.VisibleForTesting;
import lombok.NoArgsConstructor;
import org.testcontainers.UnstableAPI;

/**
 * An {@link ImageNameSubstitutor} which applies a prefix to all image names, e.g. a private registry host and path.
 * The prefix may be set via an environment variable (<code>TESTCONTAINERS_IMAGE_NAME_PREFIX</code>) or an equivalent
 * configuration file entry (see {@link TestcontainersConfiguration}).
 * <p>
 * WARNING: this class is not intended to be public, but {@link java.util.ServiceLoader}
 * requires it to be so. Public visibility DOES NOT make it part of the public API.
 */
@UnstableAPI
@NoArgsConstructor
public final class PrefixingImageNameSubstitutor extends ImageNameSubstitutor {

    @VisibleForTesting
    static final String PROPERTY_KEY = "testcontainers.image.name.prefix";

    private TestcontainersConfiguration configuration = TestcontainersConfiguration.getInstance();

    @VisibleForTesting
    PrefixingImageNameSubstitutor(final TestcontainersConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public DockerImageName apply(DockerImageName original) {
        final String prefix = configuration.getEnvVarOrProperty(PROPERTY_KEY, "");

        if (prefix != null && !prefix.isEmpty()) {
            if (!original.asCanonicalNameString().startsWith(prefix)) {
                return DockerImageName.parse(prefix + original.asCanonicalNameString());
            } else {
                return original;
            }
        } else {
            return original;
        }
    }

    @Override
    protected int getPriority() {
        return -1;
    }

    @Override
    protected String getDescription() {
        return getClass().getSimpleName();
    }
}
