package org.testcontainers.utility;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

public class ImageNameSubstitutorTest {

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
}
