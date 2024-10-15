package org.testcontainers.utility;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.testcontainers.containers.GenericContainer;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;

public class ImageNameSubstitutorTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public MockTestcontainersConfigurationRule config = new MockTestcontainersConfigurationRule();

    private ImageNameSubstitutor originalInstance;

    private ImageNameSubstitutor originalDefaultImplementation;

    @Before
    public void setUp() throws Exception {
        originalInstance = ImageNameSubstitutor.instance;
        originalDefaultImplementation = ImageNameSubstitutor.defaultImplementation;
        ImageNameSubstitutor.instance = null;
        ImageNameSubstitutor.defaultImplementation = Mockito.mock(ImageNameSubstitutor.class);

        Mockito
            .doReturn(DockerImageName.parse("substituted-image"))
            .when(ImageNameSubstitutor.defaultImplementation)
            .apply(eq(DockerImageName.parse("original")));
        Mockito.doReturn("default implementation").when(ImageNameSubstitutor.defaultImplementation).getDescription();
    }

    @After
    public void tearDown() throws Exception {
        ImageNameSubstitutor.instance = originalInstance;
        ImageNameSubstitutor.defaultImplementation = originalDefaultImplementation;
    }

    @Test
    public void simpleConfigurationTest() {
        Mockito
            .doReturn(FakeImageSubstitutor.class.getCanonicalName())
            .when(TestcontainersConfiguration.getInstance())
            .getImageSubstitutorClassName();

        final ImageNameSubstitutor imageNameSubstitutor = ImageNameSubstitutor.instance();

        DockerImageName result = imageNameSubstitutor.apply(DockerImageName.parse("original"));
        assertThat(result.asCanonicalNameString())
            .as("the image has been substituted by default then configured implementations")
            .isEqualTo("transformed-substituted-image:latest");
    }

    @Test
    public void testWorksWithoutConfiguredImplementation() {
        Mockito.doReturn(null).when(TestcontainersConfiguration.getInstance()).getImageSubstitutorClassName();

        final ImageNameSubstitutor imageNameSubstitutor = ImageNameSubstitutor.instance();

        DockerImageName result = imageNameSubstitutor.apply(DockerImageName.parse("original"));
        assertThat(result.asCanonicalNameString())
            .as("the image has been substituted by default then configured implementations")
            .isEqualTo("substituted-image:latest");
    }

    @Test
    public void testImageNameSubstitutorToString() {
        Mockito
            .doReturn(FakeImageSubstitutor.class.getCanonicalName())
            .when(TestcontainersConfiguration.getInstance())
            .getImageSubstitutorClassName();

        try (GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("original"))) {
            assertThatThrownBy(container::start)
                .hasMessageContaining(
                    "imageNameSubstitutor=Chained substitutor of 'default implementation' and then 'test implementation'"
                );
        }
    }

    @Test
    public void testImageNameSubstitutorFromServiceLoader() throws IOException {
        Path tempDir = this.tempFolder.newFolder("image-name-substitutor-test").toPath();
        Path metaInfDir = Paths.get(tempDir.toString(), "META-INF", "services");
        Files.createDirectories(metaInfDir);

        createClassFile(tempDir, "org/testcontainers/utility/ImageNameSubstitutor.class", ImageNameSubstitutor.class);
        createClassFile(tempDir, "org/testcontainers/utility/FakeImageSubstitutor.class", FakeImageSubstitutor.class);

        // Create service provider configuration file
        createServiceProviderFile(
            metaInfDir,
            "org.testcontainers.utility.ImageNameSubstitutor",
            "org.testcontainers.utility.FakeImageSubstitutor"
        );

        URL[] urls = { tempDir.toUri().toURL() };
        URLClassLoader classLoader = new URLClassLoader(urls, ImageNameSubstitutorTest.class.getClassLoader());

        final ImageNameSubstitutor imageNameSubstitutor = ImageNameSubstitutor.instance(classLoader);

        DockerImageName result = imageNameSubstitutor.apply(DockerImageName.parse("original"));
        assertThat(result.asCanonicalNameString())
            .as("the image has been substituted by default then configured implementations")
            .isEqualTo("transformed-substituted-image:latest");
    }

    private void createClassFile(Path tempDir, String classFilePath, Class<?> clazz) throws IOException {
        Path classFile = Paths.get(tempDir.toString(), classFilePath);
        Files.createDirectories(classFile.getParent());
        Files.copy(clazz.getResourceAsStream("/" + classFilePath), classFile);
    }

    private void createServiceProviderFile(Path metaInfDir, String serviceInterface, String... implementations)
        throws IOException {
        Path serviceFile = Paths.get(metaInfDir.toString(), serviceInterface);
        try (FileWriter writer = new FileWriter(serviceFile.toFile())) {
            for (String impl : implementations) {
                writer.write(impl + "\n");
            }
        }
    }
}
