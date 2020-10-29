package org.testcontainers.utility;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

/**
 * An image name substitutor converts a Docker image name, as may be specified in code, to an alternative name.
 * This is intended to provide a way to override image names, for example to enforce pulling of images from a private
 * registry.
 */
@Slf4j
public abstract class ImageNameSubstitutor implements Function<DockerImageName, DockerImageName> {

    @VisibleForTesting
    static ImageNameSubstitutor instance;

    public synchronized static ImageNameSubstitutor instance() {
        if (instance == null) {
            final String configuredClassName = TestcontainersConfiguration.getInstance().getImageSubstitutorClassName();
            log.debug("Attempting to instantiate an ImageNameSubstitutor with class: {}", configuredClassName);
            try {
                ImageNameSubstitutor configuredSubstitutor = (ImageNameSubstitutor) Class.forName(configuredClassName).getConstructor().newInstance();
                instance = wrapWithLogging(configuredSubstitutor);
            } catch (Exception e) {
                throw new IllegalArgumentException("Configured Image Substitutor could not be loaded: " + configuredClassName, e);
            }

            log.info("Using ImageNameSubstitutor: {}", instance.getDescription());
        }

        return instance;
    }

    private static ImageNameSubstitutor wrapWithLogging(final ImageNameSubstitutor wrappedInstance) {
        return new LogWrappedImageNameSubstitutor(wrappedInstance);
    }

    /**
     * Substitute a {@link DockerImageName} for another, for example to replace a generic Docker Hub image name with a
     * private registry copy of the image.
     *
     * @param original original name to be replaced
     * @return a replacement name, or the original, as appropriate
     */
    public abstract DockerImageName apply(DockerImageName original);

    protected abstract String getDescription();

    /**
     * Wrapper substitutor which logs which substitutions have been performed.
     */
    static class LogWrappedImageNameSubstitutor extends ImageNameSubstitutor {
        @VisibleForTesting
        final ImageNameSubstitutor wrappedInstance;

        public LogWrappedImageNameSubstitutor(final ImageNameSubstitutor wrappedInstance) {
            this.wrappedInstance = wrappedInstance;
        }

        @Override
        public DockerImageName apply(final DockerImageName original) {
            final String className = wrappedInstance.getClass().getName();
            final DockerImageName replacementImage = wrappedInstance.apply(original);

            if (!replacementImage.equals(original)) {
                log.info("Using {} as a substitute image for {} (using image substitutor: {})", replacementImage.asCanonicalNameString(), original.asCanonicalNameString(), className);
                return replacementImage;
            } else {
                log.debug("Did not find a substitute image for {} (using image substitutor: {})", original.asCanonicalNameString(), className);
                return original;
            }
        }

        @Override
        protected String getDescription() {
            return wrappedInstance.getDescription();
        }
    }
}
