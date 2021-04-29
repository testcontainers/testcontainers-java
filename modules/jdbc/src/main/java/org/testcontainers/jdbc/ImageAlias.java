package org.testcontainers.jdbc;

import lombok.Builder;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ImageAlias {
    private static final Map<String, ImageAliased> alias = new HashMap<>();

    public static void addAlias(String aliasName, String imageType) {
        addAlias(aliasName, imageType, null, null, null);
    }

    public static void addAlias(String aliasName, String imageType, String imageName, String imageTag) {
        addAlias(aliasName, imageType, imageName, imageTag, null);
    }

    public static void addAlias(String aliasName, String imageType, String imageName, String imageTag, String registryHost) {
        Objects.requireNonNull(aliasName);
        Objects.requireNonNull(imageType);

        ImageAliased aliased = ImageAliased.builder()
            .imageType(imageType)
            .registryHost(registryHost)
            .imageName(imageName != null ? imageName : imageType)
            .imageTag(imageTag)
            .build();
        alias.put(aliasName, aliased);
    }

    public static ImageAliased getImageAliased(String aliasName) {
        return alias.get(aliasName);
    }

    @Value
    @Builder
    public static class ImageAliased {
        String imageType;
        String registryHost;
        String imageName;
        String imageTag;
    }
}
