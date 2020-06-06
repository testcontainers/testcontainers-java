package org.testcontainers.images;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ImagePullPolicyTest {

    @ClassRule
    public static GenericContainer<?> registry = new GenericContainer<>("registry:2")
        .withExposedPorts(5000);

    private static String imageName;

    @BeforeClass
    public static void beforeClass() throws Exception {
        String testRegistryAddress = registry.getHost() + ":" + registry.getFirstMappedPort();
        String testImageName = testRegistryAddress + "/image-pull-policy-test";
        String tag = UUID.randomUUID().toString();
        imageName = testImageName + ":" + tag;

        DockerClient client = DockerClientFactory.instance().client();
        String dummySourceImage = "hello-world:latest";
        client.pullImageCmd(dummySourceImage).exec(new PullImageResultCallback()).awaitCompletion();

        String dummyImageId = client.inspectImageCmd(dummySourceImage).exec().getId();

        // push the image to the registry
        client.tagImageCmd(dummyImageId, testImageName, tag).exec();

        client.pushImageCmd(imageName)
            .exec(new PushImageResultCallback())
            .awaitCompletion(1, TimeUnit.MINUTES);
    }

    @AfterClass
    public static void afterClass() {
        try {
            DockerClientFactory.instance().client().removeImageCmd(imageName).withForce(true).exec();
        } catch (NotFoundException ignored) {
        }
    }

    @Before
    public void setUp() {
        // Clean up local cache
        try {
            DockerClientFactory.instance().client().removeImageCmd(imageName).withForce(true).exec();
        } catch (NotFoundException ignored) {
        }

        LocalImagesCache.INSTANCE.cache.remove(new DockerImageName(imageName));
    }

    @Test
    public void pullsByDefault() {
        try (
            GenericContainer<?> container = new GenericContainer<>(imageName)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        ) {
            container.start();
        }
    }

    @Test
    public void shouldAlwaysPull() {
        try (
            GenericContainer<?> container = new GenericContainer<>(imageName)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        ) {
            container.start();
        }

        DockerClientFactory.instance().client().removeImageCmd(imageName).withForce(true).exec();

        try (
            GenericContainer<?> container = new GenericContainer<>(imageName)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        ) {
            expectToFailWithNotFoundException(container);
        }

        try (
            // built_in_image_pull_policy {
            GenericContainer<?> container = new GenericContainer<>(imageName)
                .withImagePullPolicy(PullPolicy.alwaysPull())
            // }
        ) {
            container.withStartupCheckStrategy(new OneShotStartupCheckStrategy());
            container.start();
        }
    }

    @Test
    public void shouldSupportCustomPolicies() {
        try (
            // custom_image_pull_policy {
            GenericContainer<?> container = new GenericContainer<>(imageName)
                .withImagePullPolicy(new AbstractImagePullPolicy() {
                    @Override
                    protected boolean shouldPullCached(DockerImageName imageName, ImageData localImageData) {
                        return System.getenv("ALWAYS_PULL_IMAGE") != null;
                    }
                })
            // }
        ) {
            container.withStartupCheckStrategy(new OneShotStartupCheckStrategy());
            container.start();
        }
    }

    @Test
    public void shouldCheckPolicy() {
        ImagePullPolicy policy = Mockito.spy(new AbstractImagePullPolicy() {
            @Override
            protected boolean shouldPullCached(DockerImageName imageName, ImageData localImageData) {
                return false;
            }
        });
        try (
            GenericContainer<?> container = new GenericContainer<>(imageName)
                .withImagePullPolicy(policy)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
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
                    VisibleAssertions.pass("Caused by NotFoundException");
                    return;
                }
            }
            VisibleAssertions.fail("Caused by NotFoundException");
        }
    }

}
