package org.testcontainers.utility;

import lombok.Data;

@Data
class RepositoryWithVersioning {
    private static final String SHA_256 = "@sha256:";
    private static final String SEPARATOR = ":";

    private final String repository;
    private final Versioning versioning;

    static RepositoryWithVersioning from(String remoteName) {
        if (remoteName.contains(SHA_256)) {
            return new RepositoryWithVersioning(
                remoteName.split(SHA_256)[0],
                new Sha256Versioning(remoteName.split(SHA_256)[1]));
        } else if (remoteName.contains(SEPARATOR)) {
            return new RepositoryWithVersioning(
                remoteName.split(SEPARATOR)[0],
                new TagVersioning(remoteName.split(SEPARATOR)[1]));
        } else {
            return new RepositoryWithVersioning(remoteName, new TagVersioning("latest"));
        }
    }
}
