package org.testcontainers.images;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.model.Image;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class LocalImagesCacheTest {

    @BeforeEach
    @AfterEach
    void resetCache() {
        LocalImagesCacheAccessor.clearCache();
    }

    @Test
    void shouldCacheRepoDigestsAndImageIds() {
        DockerClient dockerClient = Mockito.mock(DockerClient.class);
        ListImagesCmd listImagesCmd = Mockito.mock(ListImagesCmd.class);

        when(dockerClient.listImagesCmd()).thenReturn(listImagesCmd);

        Image image = Mockito.mock(Image.class);
        when(image.getRepoTags()).thenReturn(new String[] { "test-repo:1.0", "<none>:<none>" });
        when(image.getRepoDigests())
            .thenReturn(
                new String[] {
                    "test-repo@sha256:e1594798e61a75abde649ed1432fa955853a7816f516fe49360d623213a01d96",
                    "<none>@<none>",
                }
            );
        when(image.getId()).thenReturn("sha256:e1594798e61a75abde649ed1432fa955853a7816f516fe49360d623213a01d96");
        when(image.getCreated()).thenReturn(1595874211L);

        when(listImagesCmd.exec()).thenReturn(Collections.singletonList(image));

        LocalImagesCache.INSTANCE.maybeInitCache(dockerClient);

        ImageData byTag = LocalImagesCache.INSTANCE.cache.get(DockerImageName.parse("test-repo:1.0"));
        assertThat(byTag).isNotNull();

        ImageData byDigest = LocalImagesCache.INSTANCE.cache.get(
            DockerImageName.parse("test-repo@sha256:e1594798e61a75abde649ed1432fa955853a7816f516fe49360d623213a01d96")
        );
        assertThat(byDigest).isNotNull();

        ImageData byIdWithPrefix = LocalImagesCache.INSTANCE.cache.get(
            DockerImageName.parse("sha256:e1594798e61a75abde649ed1432fa955853a7816f516fe49360d623213a01d96")
        );
        assertThat(byIdWithPrefix).isNotNull();

        ImageData byIdWithoutPrefix = LocalImagesCache.INSTANCE.cache.get(
            DockerImageName.parse("e1594798e61a75abde649ed1432fa955853a7816f516fe49360d623213a01d96")
        );
        assertThat(byIdWithoutPrefix).isNotNull();

        ImageData noneTag = LocalImagesCache.INSTANCE.cache.get(DockerImageName.parse("<none>:<none>"));
        assertThat(noneTag).isNull();
    }
}
