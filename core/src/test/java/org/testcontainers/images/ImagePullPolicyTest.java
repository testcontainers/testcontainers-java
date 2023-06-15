package org.testcontainers.images;

import com.github.dockerjava.api.exception.NotFoundException;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.DockerRegistryContainer;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;

public class ImagePullPolicyTest {

    @ClassRule
    public static DockerRegistryContainer registry = new DockerRegistryContainer();

    private final DockerImageName imageName = registry.createImage();

    @Test
    public void pullsByDefault() {
        try (GenericContainer<?> container = new GenericContainer<>(imageName).withExposedPorts(8080)) {
            container.start();
        }
    }

    @Test
    public void shouldAlwaysPull() {
        try (GenericContainer<?> container = new GenericContainer<>(imageName).withExposedPorts(8080)) {
            container.start();
        }

        removeImage();

        try (GenericContainer<?> container = new GenericContainer<>(imageName).withExposedPorts(8080)) {
            expectToFailWithNotFoundException(container);
        }

        try (
            // built_in_image_pull_policy {
            GenericContainer<?> container = new GenericContainer<>(imageName)
                .withImagePullPolicy(PullPolicy.alwaysPull())
            // }
        ) {
            container.withExposedPorts(8080);
            container.start();
        }
    }

    @Test
    public void shouldSupportCustomPolicies() {
        try (
            // custom_image_pull_policy {
            GenericContainer<?> container = new GenericContainer<>(imageName)
                .withImagePullPolicy(
                    new AbstractImagePullPolicy() {
                        @Override
                        protected boolean shouldPullCached(DockerImageName imageName, ImageData localImageData) {
                            return System.getenv("ALWAYS_PULL_IMAGE") != null;
                        }
                    }
                )
            // }
        ) {
            container.withExposedPorts(8080);
            container.start();
        }
    }

    @Test
    public void shouldCheckPolicy() {
        ImagePullPolicy policy = Mockito.spy(
            new AbstractImagePullPolicy() {
                @Override
                protected boolean shouldPullCached(DockerImageName imageName, ImageData localImageData) {
                    return false;
                }
            }
        );
        try (
            GenericContainer<?> container = new GenericContainer<>(imageName)
                .withImagePullPolicy(policy)
                .withExposedPorts(8080)
        ) {
            container.start();

            Mockito.verify(policy).shouldPull(any());
        }
    }

    @Test
    public void shouldNotForcePulling() {
        try (
            GenericContainer<?> container = new GenericContainer<>(imageName)
                .withImagePullPolicy(__ -> false)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        ) {
            expectToFailWithNotFoundException(container);
        }
    }

    private void expectToFailWithNotFoundException(GenericContainer<?> container) {
        try {
            container.start();
            fail("Should fail");
        } catch (ContainerLaunchException e) {
            Throwable throwable = e;
            while (throwable.getCause() != null) {
                throwable = throwable.getCause();
                if (throwable.getCause() instanceof NotFoundException) {
                    return;
                }
            }
            fail("Caused by NotFoundException");
        }
    }

    private void removeImage() {
        try {
            DockerClientFactory
                .instance()
                .client()
                .removeImageCmd(imageName.asCanonicalNameString())
                .withForce(true)
                .exec();
        } catch (NotFoundException ignored) {}
    }
}
