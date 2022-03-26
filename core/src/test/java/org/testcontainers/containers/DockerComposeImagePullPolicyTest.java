package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.AbstractImagePullPolicy;
import org.testcontainers.images.ImageData;
import org.testcontainers.images.ImagePullPolicy;
import org.testcontainers.images.LocalImagesCache;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.testcontainers.TestImages.DOCKER_REGISTRY_IMAGE;

public class DockerComposeImagePullPolicyTest {

    private static class PullCounter implements Consumer<OutputFrame> {

        private List<String> logs = Collections.synchronizedList(new ArrayList<>());

        public void resetCounter() {
            logs = Collections.synchronizedList(new ArrayList<>());
        }

        public long getPullCountByDockerClient(DockerImageName imageName) {
            String pullUrl = "HEAD /v2/" + imageName.getRepository() + "/manifests/" + imageName.getVersionPart();
            return logs.stream()
                .filter(log -> log.contains(pullUrl) && !log.contains("docker-compose"))
                .count();
        }

        public long getPullCountByDockerCompose(DockerImageName imageName) {
            String pullUrl = "HEAD /v2/" + imageName.getRepository() + "/manifests/" + imageName.getVersionPart();
            return logs.stream()
                .filter(log -> log.contains(pullUrl) && log.contains("docker-compose"))
                .count();
        }

        @Override
        public void accept(OutputFrame outputFrame) {
            logs.add(outputFrame.getUtf8String());
        }
    }

    private static final PullCounter pullCounter = new PullCounter();

    @ClassRule
    public static GenericContainer<?> registry = new GenericContainer<>(DOCKER_REGISTRY_IMAGE)
        .withExposedPorts(5000)
        .withLogConsumer(pullCounter);

    @ClassRule
    public static TemporaryFolder testFolder = new TemporaryFolder();

    private static DockerImageName imageName;
    private static File dockerComposeFile;

    @BeforeClass
    public static void beforeClass() throws Exception {
        String testRegistryAddress = registry.getHost() + ":" + registry.getFirstMappedPort();
        String testImageName = testRegistryAddress + "/image-pull-policy-test";
        String tag = UUID.randomUUID().toString();
        imageName = DockerImageName.parse(testImageName).withTag(tag);

        DockerClient client = DockerClientFactory.instance().client();
        String dummySourceImage = "hello-world:latest";
        client.pullImageCmd(dummySourceImage).exec(new PullImageResultCallback()).awaitCompletion();

        String dummyImageId = client.inspectImageCmd(dummySourceImage).exec().getId();

        // push the image to the registry
        client.tagImageCmd(dummyImageId, testImageName, tag).exec();

        client.pushImageCmd(imageName.asCanonicalNameString())
            .exec(new ResultCallback.Adapter<>())
            .awaitCompletion(1, TimeUnit.MINUTES);

        // create test docker-compose.yml
        dockerComposeFile = testFolder.newFile("docker-compose.yml");
        String fileContent =
            "version: \"2.1\"\n" +
                "services:\n" +
                "  hello:\n" +
                "    image: " + testImageName + ":" + tag;
        FileUtils.writeStringToFile(dockerComposeFile, fileContent, StandardCharsets.UTF_8);
    }

    @Before
    public void setUp() {
        // Clean up local cache
        removeImage();
        LocalImagesCache.INSTANCE.clearCache(imageName);

        // reset pull counter
        pullCounter.resetCounter();
    }

    @Test
    public void pullsByDefault() {
        try (
            DockerComposeContainer<?> container = new DockerComposeContainer<>(dockerComposeFile)
        ) {
            container.start();
        }

        // The local cache is empty, so DockerComposeContainer pulls the image from the remote registry
        Assert.assertEquals(1, pullCounter.getPullCountByDockerClient(imageName));
        // docker-compose doesn't pull the image when starting a container
        Assert.assertEquals(0, pullCounter.getPullCountByDockerCompose(imageName));

        pullCounter.resetCounter();
        try (
            DockerComposeContainer<?> container = new DockerComposeContainer<>(dockerComposeFile)
        ) {
            container.start();
        }

        // The image exists locally, so DockerComposeContainer doesn't pull the image from the remote registry
        Assert.assertEquals(0, pullCounter.getPullCountByDockerClient(imageName));
        // docker-compose doesn't pull the image when starting a container
        Assert.assertEquals(0, pullCounter.getPullCountByDockerCompose(imageName));
    }

    @Test
    public void shouldAlwaysPull() {
        try (
            DockerComposeContainer<?> container = new DockerComposeContainer<>(dockerComposeFile)
                .withImagePullPolicy(PullPolicy.alwaysPull())
        ) {
            container.start();
        }

        // The local cache is empty, so DockerComposeContainer pulls the image from the remote registry
        Assert.assertEquals(1, pullCounter.getPullCountByDockerClient(imageName));
        // docker-compose doesn't pull the image when starting a container
        Assert.assertEquals(0, pullCounter.getPullCountByDockerCompose(imageName));

        pullCounter.resetCounter();
        try (
            DockerComposeContainer<?> container = new DockerComposeContainer<>(dockerComposeFile)
                .withImagePullPolicy(PullPolicy.alwaysPull())
        ) {
            container.start();
        }

        // The image exists locally, but DockerComposeContainer still pulls the image from the remote registry
        Assert.assertEquals(1, pullCounter.getPullCountByDockerClient(imageName));
        // docker-compose doesn't pull the image when starting a container
        Assert.assertEquals(0, pullCounter.getPullCountByDockerCompose(imageName));
    }

    @Test
    public void shouldSupportCustomPolicies() {
        try (
            DockerComposeContainer<?> container = new DockerComposeContainer<>(dockerComposeFile)
                .withImagePullPolicy(new AbstractImagePullPolicy() {
                    @Override
                    protected boolean shouldPullCached(DockerImageName imageName, ImageData localImageData) {
                        return false;
                    }
                })
        ) {
            container.start();
        }

        // The local cache is empty, so DockerComposeContainer pulls the image from the remote registry
        Assert.assertEquals(1, pullCounter.getPullCountByDockerClient(imageName));
        // docker-compose doesn't pull the image when starting a container
        Assert.assertEquals(0, pullCounter.getPullCountByDockerCompose(imageName));
    }

    @Test
    public void shouldCheckPolicy() {
        ImagePullPolicy policy = Mockito.spy(new AbstractImagePullPolicy() {
            @Override
            protected boolean shouldPullCached(DockerImageName imageName, ImageData localImageData) {
                return true;
            }
        });

        try (
            DockerComposeContainer<?> container = new DockerComposeContainer<>(dockerComposeFile)
                .withImagePullPolicy(policy)
        ) {
            container.start();

            Mockito.verify(policy).shouldPull(any());
        }
    }

    @Test
    public void shouldNotCheckPolicyWhenPullingDisabled() {
        ImagePullPolicy policy = Mockito.spy(new AbstractImagePullPolicy() {
            @Override
            protected boolean shouldPullCached(DockerImageName imageName, ImageData localImageData) {
                return true;
            }
        });

        try (
            DockerComposeContainer<?> container = new DockerComposeContainer<>(dockerComposeFile)
                .withPull(false)
                .withImagePullPolicy(policy)
        ) {
            container.start();

            Mockito.verify(policy, Mockito.never()).shouldPull(any());
        }

        // DockerComposeImage doesn't pull the image
        Assert.assertEquals(0, pullCounter.getPullCountByDockerClient(imageName));
        // But docker-compose pulls the image when starting a container
        Assert.assertEquals(1, pullCounter.getPullCountByDockerCompose(imageName));
    }

    private static void removeImage() {
        try {
            DockerClientFactory.instance().client()
                .removeImageCmd(imageName.asCanonicalNameString())
                .withForce(true)
                .exec();

        } catch (NotFoundException ignored) {
        }
    }

}
