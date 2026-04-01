package org.testcontainers.images;

import com.github.dockerjava.api.model.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalImagesCacheTest {

    @BeforeEach
    void setUp() {
        LocalImagesCache.INSTANCE.cache.clear();
        LocalImagesCache.INSTANCE.initialized.set(false);
    }

    private Image createImage(String id, String[] repoTags, String[] repoDigests) {
        Image image = new Image();
        try {
            setField(image, "id", id);
            setField(image, "repoTags", repoTags);
            setField(image, "repoDigests", repoDigests);
            setField(image, "created", 1609459200L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return image;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void invokePopulateFromList(List<Image> images) throws Exception {
        Method method = LocalImagesCache.class.getDeclaredMethod("populateFromList", List.class);
        method.setAccessible(true);
        method.invoke(LocalImagesCache.INSTANCE, images);
    }

    @Test
    void shouldCacheByRepoTag() throws Exception {
        Image image = createImage(
            "sha256:abc123",
            new String[] { "myimage:latest" },
            null
        );

        invokePopulateFromList(Arrays.asList(image));

        assertThat(LocalImagesCache.INSTANCE.cache.get(DockerImageName.parse("myimage:latest")))
            .as("Image should be found by repo tag")
            .isNotNull();
    }

    @Test
    void shouldCacheByRepoDigest() throws Exception {
        Image image = createImage(
            "sha256:abc123def456",
            null,
            new String[] { "quay.io/testcontainers/sshd@sha256:18aa929c653284189fc9cefa45b731021857b6047a0a1757e909f958f258f088" }
        );

        invokePopulateFromList(Arrays.asList(image));

        assertThat(LocalImagesCache.INSTANCE.cache.get(
            DockerImageName.parse("quay.io/testcontainers/sshd@sha256:18aa929c653284189fc9cefa45b731021857b6047a0a1757e909f958f258f088")
        ))
            .as("Image should be found by repo digest")
            .isNotNull();
    }

    @Test
    void shouldCacheByImageId() throws Exception {
        Image image = createImage(
            "sha256:8cf620617c6203b24af7c5bf15a7212386c27ad008fc4c6ff7e37a1bf0a3cdd2",
            null,
            null
        );

        invokePopulateFromList(Arrays.asList(image));

        assertThat(LocalImagesCache.INSTANCE.cache.get(
            DockerImageName.parse("sha256:8cf620617c6203b24af7c5bf15a7212386c27ad008fc4c6ff7e37a1bf0a3cdd2")
        ))
            .as("Image should be found by image ID")
            .isNotNull();
    }

    @Test
    void shouldCacheByAllIdentifiers() throws Exception {
        Image image = createImage(
            "sha256:abc123def456789012345678901234567890",
            new String[] { "myimage:1.0" },
            new String[] { "docker.io/library/myimage@sha256:abc123def456789012345678901234567890" }
        );

        invokePopulateFromList(Arrays.asList(image));

        assertThat(LocalImagesCache.INSTANCE.cache.get(DockerImageName.parse("myimage:1.0")))
            .as("Image should be found by repo tag")
            .isNotNull();
        assertThat(LocalImagesCache.INSTANCE.cache.get(
            DockerImageName.parse("docker.io/library/myimage@sha256:abc123def456789012345678901234567890")
        ))
            .as("Image should be found by repo digest")
            .isNotNull();
        assertThat(LocalImagesCache.INSTANCE.cache.get(
            DockerImageName.parse("sha256:abc123def456789012345678901234567890")
        ))
            .as("Image should be found by image ID")
            .isNotNull();
    }

    @Test
    void shouldHandleImageWithNullRepoTagsAndDigests() throws Exception {
        Image image = createImage(
            "sha256:onlyid12345678901234567890123456",
            null,
            null
        );

        invokePopulateFromList(Arrays.asList(image));

        assertThat(LocalImagesCache.INSTANCE.cache.get(
            DockerImageName.parse("sha256:onlyid12345678901234567890123456")
        ))
            .as("Image with only ID should still be cached")
            .isNotNull();
    }

    @Test
    void shouldNotFailWhenImageIdIsNull() throws Exception {
        Image image = createImage(
            null,
            new String[] { "myimage:latest" },
            null
        );

        invokePopulateFromList(Arrays.asList(image));

        assertThat(LocalImagesCache.INSTANCE.cache.get(DockerImageName.parse("myimage:latest")))
            .as("Image should still be found by repo tag")
            .isNotNull();
    }
}
