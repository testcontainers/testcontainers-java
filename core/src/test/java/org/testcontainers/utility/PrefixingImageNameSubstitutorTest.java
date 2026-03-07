package org.testcontainers.utility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrefixingImageNameSubstitutorTest {

    private TestcontainersConfiguration mockConfiguration;

    private PrefixingImageNameSubstitutor underTest;

    @BeforeEach
    public void setUp() {
        mockConfiguration = mock(TestcontainersConfiguration.class);
        underTest = new PrefixingImageNameSubstitutor(mockConfiguration);
    }

    @Test
    void testHappyPath() {
        when(mockConfiguration.getEnvVarOrProperty(eq(PrefixingImageNameSubstitutor.PREFIX_PROPERTY_KEY), any()))
            .thenReturn("someregistry.com/our-mirror/");

        final DockerImageName result = underTest.apply(DockerImageName.parse("some/image:tag"));

        assertThat(result.asCanonicalNameString())
            .as("The prefix is applied")
            .isEqualTo("someregistry.com/our-mirror/some/image:tag");
    }

    @Test
    void hubIoRegistryIsNotChanged() {
        when(mockConfiguration.getEnvVarOrProperty(eq(PrefixingImageNameSubstitutor.PREFIX_PROPERTY_KEY), any()))
            .thenReturn("someregistry.com/our-mirror/");

        final DockerImageName result = underTest.apply(DockerImageName.parse("docker.io/some/image:tag"));

        assertThat(result.asCanonicalNameString()).as("The prefix is applied").isEqualTo("docker.io/some/image:tag");
    }

    @Test
    void hubComRegistryIsNotChanged() {
        when(mockConfiguration.getEnvVarOrProperty(eq(PrefixingImageNameSubstitutor.PREFIX_PROPERTY_KEY), any()))
            .thenReturn("someregistry.com/our-mirror/");

        final DockerImageName result = underTest.apply(DockerImageName.parse("registry.hub.docker.com/some/image:tag"));

        assertThat(result.asCanonicalNameString())
            .as("The prefix is applied")
            .isEqualTo("registry.hub.docker.com/some/image:tag");
    }

    @Test
    void thirdPartyRegistriesNotAffected() {
        when(mockConfiguration.getEnvVarOrProperty(eq(PrefixingImageNameSubstitutor.PREFIX_PROPERTY_KEY), any()))
            .thenReturn("someregistry.com/our-mirror/");

        final DockerImageName result = underTest.apply(DockerImageName.parse("gcr.io/something/image:tag"));

        assertThat(result.asCanonicalNameString())
            .as("The prefix is not applied if a third party registry is used")
            .isEqualTo("gcr.io/something/image:tag");
    }

    @Test
    void testNoDoublePrefixing() {
        when(mockConfiguration.getEnvVarOrProperty(eq(PrefixingImageNameSubstitutor.PREFIX_PROPERTY_KEY), any()))
            .thenReturn("someregistry.com/our-mirror/");

        final DockerImageName result = underTest.apply(DockerImageName.parse("someregistry.com/some/image:tag"));

        assertThat(result.asCanonicalNameString())
            .as("The prefix is not applied if already present")
            .isEqualTo("someregistry.com/some/image:tag");
    }

    @Test
    void testHandlesEmptyValue() {
        when(mockConfiguration.getEnvVarOrProperty(eq(PrefixingImageNameSubstitutor.PREFIX_PROPERTY_KEY), any()))
            .thenReturn("");

        final DockerImageName result = underTest.apply(DockerImageName.parse("some/image:tag"));

        assertThat(result.asCanonicalNameString())
            .as("The prefix is not applied if the env var is not set")
            .isEqualTo("some/image:tag");
    }

    @Test
    void testHandlesRegistryOnlyWithTrailingSlash() {
        when(mockConfiguration.getEnvVarOrProperty(eq(PrefixingImageNameSubstitutor.PREFIX_PROPERTY_KEY), any()))
            .thenReturn("someregistry.com/");

        final DockerImageName result = underTest.apply(DockerImageName.parse("some/image:tag"));

        assertThat(result.asCanonicalNameString())
            .as("The prefix is applied")
            .isEqualTo("someregistry.com/some/image:tag");
    }

    @Test
    void testCombinesLiterallyForRegistryOnlyWithoutTrailingSlash() {
        when(mockConfiguration.getEnvVarOrProperty(eq(PrefixingImageNameSubstitutor.PREFIX_PROPERTY_KEY), any()))
            .thenReturn("someregistry.com");

        final DockerImageName result = underTest.apply(DockerImageName.parse("some/image:tag"));

        assertThat(result.asCanonicalNameString())
            .as("The prefix is applied")
            .isEqualTo("someregistry.comsome/image:tag");
    }

    @Test
    void testCombinesLiterallyForBothPartsWithoutTrailingSlash() {
        when(mockConfiguration.getEnvVarOrProperty(eq(PrefixingImageNameSubstitutor.PREFIX_PROPERTY_KEY), any()))
            .thenReturn("someregistry.com/our-mirror");

        final DockerImageName result = underTest.apply(DockerImageName.parse("some/image:tag"));

        assertThat(result.asCanonicalNameString())
            .as("The prefix is applied")
            .isEqualTo("someregistry.com/our-mirrorsome/image:tag");
    }
}
