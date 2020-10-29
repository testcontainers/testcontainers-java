package org.testcontainers.utility;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.testcontainers.utility.PrefixingImageNameSubstitutor.PROPERTY_KEY;

public class PrefixingImageNameSubstitutorTest {

    private TestcontainersConfiguration mockConfiguration;
    private PrefixingImageNameSubstitutor underTest;

    @Before
    public void setUp() {
        mockConfiguration = mock(TestcontainersConfiguration.class);
        underTest = new PrefixingImageNameSubstitutor(mockConfiguration);
    }

    @Test
    public void testHappyPath() {
        when(mockConfiguration.getEnvVarOrProperty(eq(PROPERTY_KEY), any())).thenReturn("someregistry.com/");

        final DockerImageName result = underTest.apply(DockerImageName.parse("some/image:tag"));

        assertEquals(
            "The prefix is applied",
            "someregistry.com/some/image:tag",
            result.asCanonicalNameString()
        );
    }

    @Test
    public void testNoDoublePrefixing() {
        when(mockConfiguration.getEnvVarOrProperty(eq(PROPERTY_KEY), any())).thenReturn("someregistry.com/");

        final DockerImageName result = underTest.apply(DockerImageName.parse("someregistry.com/some/image:tag"));

        assertEquals(
            "The prefix is not applied if already present",
            "someregistry.com/some/image:tag",
            result.asCanonicalNameString()
        );
    }

    @Test
    public void testHandlesNullValue() {
        when(mockConfiguration.getEnvVarOrProperty(eq(PROPERTY_KEY), any())).thenReturn(null);

        final DockerImageName result = underTest.apply(DockerImageName.parse("some/image:tag"));

        assertEquals(
            "The prefix is not applied if the env var is not set",
            "some/image:tag",
            result.asCanonicalNameString()
        );
    }

    @Test
    public void testHandlesEmptyValue() {
        when(mockConfiguration.getEnvVarOrProperty(eq(PROPERTY_KEY), any())).thenReturn("");

        final DockerImageName result = underTest.apply(DockerImageName.parse("some/image:tag"));

        assertEquals(
            "The prefix is not applied if the env var is not set",
            "some/image:tag",
            result.asCanonicalNameString()
        );
    }
}
