package org.testcontainers.images;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.LazyFuture;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteDockerImageTest {

    private static final String HUB_IMAGE_NAME_PREFIX_PROPERTY = "hub.image.name.prefix";

    private static final String HUB_IMAGE_NAME_PREFIX_ENV_VAR = "TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX";

    private static final String HUB_IMAGE_NAME_PREFIX = "registry.example.com/mirror/";

    private static final String LOCAL_IMAGE_NAME = "redis-sentinel:latest";

    @Test
    void futureBackedLocalImageDoesNotUseConfiguredHubPrefix() throws Exception {
        RemoteDockerImage image = new RemoteDockerImage(CompletableFuture.completedFuture(LOCAL_IMAGE_NAME));

        assertThat(resolveWithConfiguredHubPrefix(image)).isEqualTo(LOCAL_IMAGE_NAME);
    }

    @Test
    void futureBackedImageWithExplicitRegistryDoesNotUseConfiguredHubPrefix() throws Exception {
        String imageName = "localhost:5000/redis-sentinel:latest";
        RemoteDockerImage image = new RemoteDockerImage(CompletableFuture.completedFuture(imageName));

        assertThat(resolveWithConfiguredHubPrefix(image)).isEqualTo(imageName);
    }

    @Test
    void dockerImageNameUsesConfiguredHubPrefix() throws Exception {
        RemoteDockerImage image = new RemoteDockerImage(DockerImageName.parse(LOCAL_IMAGE_NAME));

        assertThat(resolveWithConfiguredHubPrefix(image)).isEqualTo(HUB_IMAGE_NAME_PREFIX + LOCAL_IMAGE_NAME);
    }

    @Test
    void toStringContainsOnlyImageName() throws Exception {
        String imageName = Base58.randomString(8).toLowerCase();
        RemoteDockerImage remoteDockerImage = new RemoteDockerImage(DockerImageName.parse(imageName));
        assertThat(withPinnedHubImageNamePrefix("", remoteDockerImage::toString)).contains("imageName=" + imageName);
    }

    @Test
    void toStringWithExceptionContainsOnlyImageNameFuture() {
        CompletableFuture<String> imageNameFuture = new CompletableFuture<>();
        imageNameFuture.completeExceptionally(new RuntimeException("arbitrary"));

        RemoteDockerImage remoteDockerImage = new RemoteDockerImage(imageNameFuture);
        assertThat(remoteDockerImage.toString()).contains("imageName=java.lang.RuntimeException: arbitrary");
    }

    @Test
    @Timeout(5)
    void toStringDoesntResolveImageNameFuture() {
        CompletableFuture<String> imageNameFuture = new CompletableFuture<>();

        // verify that we've set up the test properly
        assertThat(imageNameFuture).isNotDone();

        RemoteDockerImage remoteDockerImage = new RemoteDockerImage(imageNameFuture);
        assertThat(remoteDockerImage.toString()).contains("imageName=<resolving>");

        // Make sure the act of calling toString doesn't resolve the imageNameFuture
        assertThat(imageNameFuture).isNotDone();

        String imageName = Base58.randomString(8).toLowerCase();
        imageNameFuture.complete(imageName);
        assertThat(remoteDockerImage.toString()).contains("imageName=" + imageName);
    }

    @Test
    @Timeout(5)
    void toStringDoesntResolveLazyFuture() throws Exception {
        String imageName = Base58.randomString(8).toLowerCase();
        AtomicBoolean resolved = new AtomicBoolean(false);
        Future<String> imageNameFuture = new LazyFuture<String>() {
            @Override
            protected String resolve() {
                resolved.set(true);
                return imageName;
            }
        };

        // verify that we've set up the test properly
        assertThat(imageNameFuture).isNotDone();

        RemoteDockerImage remoteDockerImage = new RemoteDockerImage(imageNameFuture);
        assertThat(remoteDockerImage.toString()).contains("imageName=<resolving>");

        // Make sure the act of calling toString doesn't resolve the imageNameFuture
        assertThat(imageNameFuture).isNotDone();
        assertThat(resolved).isFalse();

        // Trigger resolve
        imageNameFuture.get();
        assertThat(remoteDockerImage.toString()).contains("imageName=" + imageName);
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Runs the given action with the {@code hub.image.name.prefix} configuration pinned to
     * {@code prefix}, isolated from machine-level settings: environment variables take precedence
     * over user properties, so a {@code TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX} variable set on the
     * machine or CI job is temporarily removed from the configuration's environment copy.
     */
    private <T> T withPinnedHubImageNamePrefix(String prefix, ThrowingSupplier<T> action) throws Exception {
        TestcontainersConfiguration configuration = TestcontainersConfiguration.getInstance();

        // Avoid TestcontainersConfiguration.updateUserConfig, which writes to the user's configuration file.
        Field userPropertiesField = TestcontainersConfiguration.class.getDeclaredField("userProperties");
        userPropertiesField.setAccessible(true);
        Properties userProperties = (Properties) userPropertiesField.get(configuration);
        String originalPrefix = userProperties.getProperty(HUB_IMAGE_NAME_PREFIX_PROPERTY);

        Field environmentField = TestcontainersConfiguration.class.getDeclaredField("environment");
        environmentField.setAccessible(true);
        Map<String, String> originalEnvironment = (Map<String, String>) environmentField.get(configuration);
        Map<String, String> pinnedEnvironment = new HashMap<>(originalEnvironment);
        pinnedEnvironment.remove(HUB_IMAGE_NAME_PREFIX_ENV_VAR);

        try {
            userProperties.setProperty(HUB_IMAGE_NAME_PREFIX_PROPERTY, prefix);
            environmentField.set(configuration, pinnedEnvironment);
            return action.get();
        } finally {
            environmentField.set(configuration, originalEnvironment);
            if (originalPrefix == null) {
                userProperties.remove(HUB_IMAGE_NAME_PREFIX_PROPERTY);
            } else {
                userProperties.setProperty(HUB_IMAGE_NAME_PREFIX_PROPERTY, originalPrefix);
            }
        }
    }

    private String resolveWithConfiguredHubPrefix(RemoteDockerImage image) throws Exception {
        return withPinnedHubImageNamePrefix(HUB_IMAGE_NAME_PREFIX, () -> image.withImagePullPolicy(__ -> false).get());
    }
}
