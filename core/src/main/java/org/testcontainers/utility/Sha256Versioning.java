package org.testcontainers.utility;

import lombok.Data;

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
