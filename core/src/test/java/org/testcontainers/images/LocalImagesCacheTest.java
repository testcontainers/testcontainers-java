package org.testcontainers.images;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LocalImagesCacheTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalImagesCache.INSTANCE.cache.clear();
        LocalImagesCache.INSTANCE.initialized.set(false);
    }

    @Test
    void shouldCacheImageByRepoTag() {
        Image image = createImage(new String[] { "alpine:3.17" }, null, "sha256:aaa111", 1000L);

        populateCache(image);

        assertThat(LocalImagesCache.INSTANCE.cache).containsKey(new DockerImageName("alpine:3.17"));
    }

    @Test
    void shouldCacheImageByRepoDigest() {
        Image image = createImage(
            null,
            new String[] { "alpine@sha256:1775bebec23e1f3ce486989bfc9ff3c4e951690df84aa9f926497d82f2ffca9d" },
            "sha256:bbb222",
            1000L
        );

        populateCache(image);

        assertThat(LocalImagesCache.INSTANCE.cache)
            .containsKey(
                new DockerImageName("alpine@sha256:1775bebec23e1f3ce486989bfc9ff3c4e951690df84aa9f926497d82f2ffca9d")
            );
    }

    @Test
    void shouldCacheImageByBothTagAndDigest() {
        Image image = createImage(
            new String[] { "alpine:3.17" },
            new String[] { "alpine@sha256:1775bebec23e1f3ce486989bfc9ff3c4e951690df84aa9f926497d82f2ffca9d" },
            "sha256:ccc333",
            1000L
        );

        populateCache(image);

        assertThat(LocalImagesCache.INSTANCE.cache)
            .containsKey(new DockerImageName("alpine:3.17"))
            .containsKey(
                new DockerImageName("alpine@sha256:1775bebec23e1f3ce486989bfc9ff3c4e951690df84aa9f926497d82f2ffca9d")
            );
    }

    @Test
    void shouldSkipImageWithNullTagsAndNullDigestsAndNullId() {
        Image image = createImage(null, null, null, 1000L);

        populateCache(image);

        assertThat(LocalImagesCache.INSTANCE.cache).isEmpty();
    }

    @Test
    void shouldHandleMultipleDigestsForSameImage() {
        Image image = createImage(
            new String[] { "myapp:latest" },
            new String[] {
                "myapp@sha256:aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111",
                "registry.example.com/myapp@sha256:bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222",
            },
            "sha256:eee555",
            1000L
        );

        populateCache(image);

        assertThat(LocalImagesCache.INSTANCE.cache)
            .containsKey(new DockerImageName("myapp:latest"))
            .containsKey(
                new DockerImageName("myapp@sha256:aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111aaaa1111")
            )
            .containsKey(
                new DockerImageName(
                    "registry.example.com/myapp@sha256:bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222bbbb2222"
                )
            );
    }

    @Test
    void shouldHandleDuplicateRepoTags() {
        Image image = createImage(new String[] { "alpine:3.17", "alpine:3.17" }, null, "sha256:fff666", 1000L);

        populateCache(image);

        assertThat(LocalImagesCache.INSTANCE.cache).containsKey(new DockerImageName("alpine:3.17"));
    }

    @Test
    void shouldCacheImageById() {
        Image image = createImage(
            new String[] { "alpine:3.17" },
            null,
            "sha256:8cf620617c6203b24af7c5bf15a7212386c27ad008fc4c6ff7e37a1bf0a3cdd2",
            1000L
        );

        populateCache(image);

        assertThat(LocalImagesCache.INSTANCE.cache)
            .containsKey(new DockerImageName("alpine:3.17"))
            .containsKey(
                new DockerImageName("sha256:8cf620617c6203b24af7c5bf15a7212386c27ad008fc4c6ff7e37a1bf0a3cdd2")
            );
    }

    @Test
    void shouldCacheImageByIdWhenNoTagsOrDigests() {
        Image image = createImage(
            null,
            null,
            "sha256:8cf620617c6203b24af7c5bf15a7212386c27ad008fc4c6ff7e37a1bf0a3cdd2",
            1000L
        );

        populateCache(image);

        assertThat(LocalImagesCache.INSTANCE.cache)
            .hasSize(1)
            .containsKey(
                new DockerImageName("sha256:8cf620617c6203b24af7c5bf15a7212386c27ad008fc4c6ff7e37a1bf0a3cdd2")
            );
    }

    @Test
    void shouldCacheDigestOnlyImageWithoutTags() {
        Image image = createImage(
            new String[] { "<none>:<none>" },
            new String[] { "myrepo@sha256:abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234" },
            "sha256:ggg777",
            1000L
        );

        populateCache(image);

        // The <none>:<none> tag entry may fail to be useful, but the digest entry should be present
        assertThat(LocalImagesCache.INSTANCE.cache)
            .containsKey(
                new DockerImageName("myrepo@sha256:abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234")
            );
    }

    private static Image createImage(String[] repoTags, String[] repoDigests, String id, Long created) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("RepoTags", repoTags);
        fields.put("RepoDigests", repoDigests);
        fields.put("Id", id);
        fields.put("Created", created);
        return MAPPER.convertValue(fields, Image.class);
    }

    private static void populateCache(Image... images) {
        try {
            java.lang.reflect.Method method =
                LocalImagesCache.class.getDeclaredMethod("populateFromList", java.util.List.class);
            method.setAccessible(true);
            method.invoke(LocalImagesCache.INSTANCE, Arrays.asList(images));
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke populateFromList", e);
        }
    }
}
