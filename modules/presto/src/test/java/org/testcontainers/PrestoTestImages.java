package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface PrestoTestImages {
    DockerImageName PRESTO_TEST_IMAGE = DockerImageName.parse("ghcr.io/trinodb/presto:344");
    DockerImageName PRESTO_PREVIOUS_VERSION_TEST_IMAGE = DockerImageName.parse("ghcr.io/trinodb/presto:343");
}
