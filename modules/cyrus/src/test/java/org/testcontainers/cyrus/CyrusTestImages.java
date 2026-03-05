package org.testcontainers.cyrus;

import org.testcontainers.utility.DockerImageName;

public interface CyrusTestImages {
    String CYRUS_IMAGE_NAME =
        "ghcr.io/cyrusimap/cyrus-docker-test-server@sha256:d639a9116691a7a1c875073486c419d60843e5ef8e32e65c5ef56283874dbf2c";

    DockerImageName CYRUS_IMAGE = DockerImageName.parse(CYRUS_IMAGE_NAME);
}
