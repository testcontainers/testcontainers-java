package org.testcontainers.utility;

import lombok.Data;

/**
 * Represents mechanisms for versioning docker images.
 */
interface Versioning {
    boolean isValid();

    String getSeparator();

    @Data
    class TagVersioning implements Versioning {
        public static final String TAG_REGEX = "[\\w][\\w.\\-]{0,127}";
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

        static final TagVersioning LATEST = new TagVersioning("latest");
    }

    @Data
    class Sha256Versioning implements Versioning {
        public static final String HASH_REGEX = "[0-9a-fA-F]{32,}";
        private final String hash;

        Sha256Versioning(String hash) {
            this.hash = hash;
        }

        @Override
        public boolean isValid() {
            return hash.matches(HASH_REGEX);
        }

        @Override
        public String getSeparator() {
            return "@";
        }

        @Override
        public String toString() {
            return "sha256:" + hash;
        }
    }
}
