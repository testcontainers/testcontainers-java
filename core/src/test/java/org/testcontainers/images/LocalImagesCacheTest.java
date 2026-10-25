package org.testcontainers.images;

import com.github.dockerjava.api.model.Image;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class LocalImagesCacheTest {

    private static final String DIGEST =
        "postgres@sha256:0000000000000000000000000000000000000000000000000000000000000001";

    @AfterEach
    void tearDown() {
        LocalImagesCacheAccessor.clearCache();
    }

    @Test
    void shouldCacheImagePulledByDigestOnly() {
        Image image = Mockito.mock(Image.class);
        Mockito.when(image.getRepoTags()).thenReturn(null);
        Mockito.when(image.getRepoDigests()).thenReturn(new String[] { DIGEST });

        LocalImagesCache.INSTANCE.populateFromList(Collections.singletonList(image));

        assertThat(LocalImagesCache.INSTANCE.cache).containsKey(new DockerImageName(DIGEST));
    }

    @Test
    void shouldCacheImageByTagAndDigest() {
        Image image = Mockito.mock(Image.class);
        Mockito.when(image.getRepoTags()).thenReturn(new String[] { "postgres:16" });
        Mockito.when(image.getRepoDigests()).thenReturn(new String[] { DIGEST });

        LocalImagesCache.INSTANCE.populateFromList(Collections.singletonList(image));

        assertThat(LocalImagesCache.INSTANCE.cache)
            .containsKeys(new DockerImageName("postgres:16"), new DockerImageName(DIGEST));
    }

    @Test
    void shouldSkipImageWithoutTagsAndDigests() {
        Image image = Mockito.mock(Image.class);
        Mockito.when(image.getRepoTags()).thenReturn(null);
        Mockito.when(image.getRepoDigests()).thenReturn(null);

        LocalImagesCache.INSTANCE.populateFromList(Collections.singletonList(image));

        assertThat(LocalImagesCache.INSTANCE.cache).isEmpty();
    }
}
