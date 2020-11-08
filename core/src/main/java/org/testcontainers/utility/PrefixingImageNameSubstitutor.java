package org.testcontainers.utility;

import com.google.common.annotations.VisibleForTesting;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.UnstableAPI;

/**
 * An {@link ImageNameSubstitutor} which applies a prefix to all image names, e.g. a private registry host and path.
 * The prefix may be set via an environment variable (<code>TESTCONTAINERS_IMAGE_NAME_PREFIX</code>) or an equivalent
 * configuration file entry (see {@link TestcontainersConfiguration}).
 */
@UnstableAPI
@NoArgsConstructor
@Slf4j
final class PrefixingImageNameSubstitutor extends ImageNameSubstitutor {

    @VisibleForTesting
    static final String REGISTRY_PROPERTY_KEY = "hub.image.override.registry";
    static final String REPOSITORY_PREFIX_PROPERTY_KEY = "hub.image.override.repository.prefix";

    private TestcontainersConfiguration configuration = TestcontainersConfiguration.getInstance();

    @VisibleForTesting
    PrefixingImageNameSubstitutor(final TestcontainersConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public DockerImageName apply(DockerImageName original) {
        final String registryOverride = configuration.getEnvVarOrProperty(REGISTRY_PROPERTY_KEY, "");
        final String repositoryPrefixOrEmpty = configuration.getEnvVarOrProperty(REPOSITORY_PREFIX_PROPERTY_KEY, "");
        boolean overrideIsConfigured = !registryOverride.isEmpty();

        if (!overrideIsConfigured) {
            log.debug("No override is configured");
            return original;
        }

        boolean isAHubImage = original.getRegistry().isEmpty() ||
            original.getRegistry().equals("docker.io") ||
            original.getRegistry().equals("registry.hub.docker.com");
        if (!isAHubImage) {
            log.debug("Image {} is not a Docker Hub image - not applying registry/repository change", original);
            return original;
        }

        log.debug(
            "Applying changes to image name {}: Changing registry part to '{}' and applying prefix '{}' to repository name part",
            original,
            registryOverride,
            repositoryPrefixOrEmpty
        );

        return original
            .withRegistry(registryOverride)
            .withRepository(repositoryPrefixOrEmpty + original.getRepository());
    }

    @Override
    protected String getDescription() {
        return getClass().getSimpleName();
    }
}
