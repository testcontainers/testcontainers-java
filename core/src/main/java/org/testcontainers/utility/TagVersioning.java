package org.testcontainers.utility;

import lombok.Data;

@Data
class TagVersioning implements Versioning {
    public static final String TAG_REGEX = "[\\w][\\w\\.\\-]{0,127}";
    private final String tag;

    TagVersioning(String tag) {
        this.tag = tag;
    }

    @Override
    public boolean isValid() {
        return tag.matches(TAG_REGEX);
    }

    @Override
    public String getSeparator() {
        return ":";
    }

    @Override
    public String toString() {
        return tag;
    }
}
