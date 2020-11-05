package org.testcontainers.utility;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;

public class DefaultImageNameSubstitutorTest {

    public static final DockerImageName ORIGINAL_IMAGE = DockerImageName.parse("foo");
    public static final DockerImageName SUBSTITUTE_IMAGE = DockerImageName.parse("bar");
    private ConfigurationFileImageNameSubstitutor underTest;

    @Rule
    public MockTestcontainersConfigurationRule config = new MockTestcontainersConfigurationRule();

    @Before
    public void setUp() {
        underTest = new ConfigurationFileImageNameSubstitutor(TestcontainersConfiguration.getInstance());
    }

    @Test
    public void testConfigurationLookup() {
        Mockito
            .doReturn(SUBSTITUTE_IMAGE)
            .when(TestcontainersConfiguration.getInstance())
            .getConfiguredSubstituteImage(eq(ORIGINAL_IMAGE));

        final DockerImageName substitute = underTest.apply(ORIGINAL_IMAGE);

        assertEquals("match is found", SUBSTITUTE_IMAGE, substitute);
        assertTrue("compatibility is automatically set", substitute.isCompatibleWith(ORIGINAL_IMAGE));
    }
}
