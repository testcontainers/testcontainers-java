package org.testcontainers.images;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.Image;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LocalImagesCacheTest {

    @AfterEach
    void tearDown() {
        LocalImagesCacheAccessor.clearCache();
    }

    @Test
    void shouldIndexImageByRepoDigestEvenWhenRepoTagsIsNull() {
        Image image = imageWith(null, new String[] { "example.com/my/image@sha256:" + "a".repeat(64) });

        LocalImagesCache.INSTANCE.populateFromList(Collections.singletonList(image));

        DockerImageName byDigest = DockerImageName.parse("example.com/my/image@sha256:" + "a".repeat(64));
        assertThat(LocalImagesCache.INSTANCE.cache).containsKey(byDigest);
    }

    @Test
    void shouldIndexImageByBothRepoTagsAndRepoDigests() {
        Image image = imageWith(
            new String[] { "example.com/my/image:latest" },
            new String[] { "example.com/my/image@sha256:" + "b".repeat(64) }
        );

        LocalImagesCache.INSTANCE.populateFromList(Collections.singletonList(image));

        assertThat(LocalImagesCache.INSTANCE.cache)
            .containsKey(DockerImageName.parse("example.com/my/image:latest"))
            .containsKey(DockerImageName.parse("example.com/my/image@sha256:" + "b".repeat(64)));
    }

    @Test
    void shouldSkipImageWithNeitherRepoTagsNorRepoDigests() {
        Image image = imageWith(null, null);

        LocalImagesCache.INSTANCE.populateFromList(Collections.singletonList(image));

        assertThat(LocalImagesCache.INSTANCE.cache).isEmpty();
    }

    private static Image imageWith(String[] repoTags, String[] repoDigests) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("Id", "sha256:" + "c".repeat(64));
        if (repoTags != null) {
            fields.put("RepoTags", Arrays.asList(repoTags));
        }
        if (repoDigests != null) {
            fields.put("RepoDigests", Arrays.asList(repoDigests));
        }
        return new ObjectMapper().convertValue(fields, Image.class);
    }
}
