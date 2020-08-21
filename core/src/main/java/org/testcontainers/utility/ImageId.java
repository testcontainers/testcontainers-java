package org.testcontainers.utility;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public final class ImageId {

    private final String imageId;

    private ImageId(String imageId) {
        this.imageId = imageId;
    }

    public static ImageId fromString(String imageId) {
        return new ImageId(imageId);
    }

    public String getValue() {
        return imageId;
    }
}
