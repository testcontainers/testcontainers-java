package org.testcontainers.utility;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.AuthConfig;
import org.intellij.lang.annotations.Language;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.TestImages.DOCKER_REGISTRY_IMAGE;

/**
 * This test checks the integration between Testcontainers and an authenticated registry, but uses
 * a mock instance of {@link RegistryAuthLocator} - the purpose of the test is solely to ensure that
 * the auth locator is utilised, and that the credentials it provides flow through to the registry.
 * <p>
 * {@link RegistryAuthLocatorTest} covers actual credential scenarios at a lower level, which are
 * impractical to test end-to-end.
 */
public class AuthenticatedImagePullTest {

    /**
     * Containerised docker image registry, with simple hardcoded credentials
     */
    @ClassRule
    public static GenericContainer<?> authenticatedRegistry = new GenericContainer<>(new ImageFromDockerfile()
        .withDockerfileFromBuilder(builder -> {
            builder.from(DOCKER_REGISTRY_IMAGE.asCanonicalNameString())
                .run("htpasswd -Bbn testuser notasecret > /htpasswd")
                .env("REGISTRY_AUTH", "htpasswd")
                .env("REGISTRY_AUTH_HTPASSWD_PATH", "/htpasswd")
                .env("REGISTRY_AUTH_HTPASSWD_REALM", "Test");
        }))
        .withExposedPorts(5000)
        .waitingFor(new HttpWaitStrategy());

    private static RegistryAuthLocator originalAuthLocatorSingleton;
    private static DockerClient client;

    private static String testImageName;
    private static String testImageNameWithTag;

    @BeforeClass
    public static void setUp() throws InterruptedException {
        originalAuthLocatorSingleton = RegistryAuthLocator.instance();
        client = DockerClientFactory.instance().client();

        String testRegistryAddress = authenticatedRegistry.getHost() + ":" + authenticatedRegistry.getFirstMappedPort();
        testImageName = testRegistryAddress + "/alpine";
        testImageNameWithTag = testImageName + ":latest";

        final DockerImageName expectedName = DockerImageName.parse(testImageNameWithTag);
        final AuthConfig authConfig = new AuthConfig()
            .withUsername("testuser")
            .withPassword("notasecret")
            .withRegistryAddress("http://" + testRegistryAddress);

        // Replace the RegistryAuthLocator singleton with our mock, for the duration of this test
        final RegistryAuthLocator mockAuthLocator = Mockito.mock(RegistryAuthLocator.class);
        RegistryAuthLocator.setInstance(mockAuthLocator);
        when(mockAuthLocator.lookupAuthConfig(eq(expectedName), any()))
            .thenReturn(authConfig);

        // a push will use the auth locator for authentication, although that isn't the goal of this test
        putImageInRegistry();
    }

    @Before
    public void removeImageFromLocalDocker() {
        // remove the image tag from local docker so that it must be pulled before use
        client.removeImageCmd(testImageNameWithTag).withForce(true).exec();
    }

    @AfterClass
    public static void tearDown() {
        RegistryAuthLocator.setInstance(originalAuthLocatorSingleton);
    }

    @Test
    public void testThatAuthLocatorIsUsedForContainerCreation() {
        // actually start a container, which will require an authenticated pull
        try (final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(testImageNameWithTag))
            .withCommand("/bin/sh", "-c", "sleep 10")) {
            container.start();

            assertTrue("container started following an authenticated pull", container.isRunning());
        }
    }

    @Test
    public void testThatAuthLocatorIsUsedForDockerfileBuild() throws IOException {
        // Prepare a simple temporary Dockerfile which requires our custom private image
        Path tempFile = getLocalTempFile(".Dockerfile");
        String dockerFileContent = "FROM " + testImageNameWithTag;
        Files.write(tempFile, dockerFileContent.getBytes());

        // Start a container built from a derived image, which will require an authenticated pull
        try (final GenericContainer<?> container = new GenericContainer<>(
            new ImageFromDockerfile()
                .withDockerfile(tempFile)
        )
            .withCommand("/bin/sh", "-c", "sleep 10")) {
            container.start();

            assertTrue("container started following an authenticated pull", container.isRunning());
        }
    }

    @Test
    public void testThatAuthLocatorIsUsedForDockerComposePull() throws IOException {
        // Prepare a simple temporary Docker Compose manifest which requires our custom private image
        Path tempFile = getLocalTempFile(".docker-compose.yml");
        @Language("yaml") String composeFileContent =
            "version: '2.0'\n" +
                "services:\n" +
                "  privateservice:\n" +
                "      command: /bin/sh -c 'sleep 60'\n" +
                "      image: " + testImageNameWithTag;
        Files.write(tempFile, composeFileContent.getBytes());

        // Start the docker compose project, which will require an authenticated pull
        try (final DockerComposeContainer<?> compose = new DockerComposeContainer<>(tempFile.toFile())) {
            compose.start();

            assertTrue("container started following an authenticated pull",
                compose
                    .getContainerByServiceName("privateservice_1")
                    .map(ContainerState::isRunning)
                    .orElse(false)
            );
        }
    }

    private Path getLocalTempFile(String s) throws IOException {
        Path projectRoot = Paths.get(".");
        Path tempDirectory = Files.createTempDirectory(projectRoot, this.getClass().getSimpleName() + "-test-");
        Path relativeTempDirectory = projectRoot.relativize(tempDirectory);
        Path tempFile = Files.createTempFile(relativeTempDirectory, "test", s);

        tempDirectory.toFile().deleteOnExit();
        tempFile.toFile().deleteOnExit();

        return tempFile;
    }

    private static void putImageInRegistry() throws InterruptedException {
        // It doesn't matter which image we use for this test, but use one that's likely to have been pulled already
        final String dummySourceImage = TestcontainersConfiguration.getInstance().getRyukDockerImageName().asCanonicalNameString();

        client.pullImageCmd(dummySourceImage)
            .exec(new PullImageResultCallback())
            .awaitCompletion(1, TimeUnit.MINUTES);

        final String id = client.inspectImageCmd(dummySourceImage)
            .exec()
            .getId();

        // push the image to the registry
        client.tagImageCmd(id, testImageName, "latest").exec();

        client.pushImageCmd(testImageNameWithTag)
            .exec(new ResultCallback.Adapter<>())
            .awaitCompletion(1, TimeUnit.MINUTES);
    }
}
