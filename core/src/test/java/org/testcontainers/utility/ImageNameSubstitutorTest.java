package org.testcontainers.utility;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockTestcontainersConfigurationExtension.class)
public class ImageNameSubstitutorTest {

    private ImageNameSubstitutor originalInstance;

    private ImageNameSubstitutor originalDefaultImplementation;

    @BeforeEach
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

    @AfterEach
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
}
