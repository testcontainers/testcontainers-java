package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface TrinoTestImages {
    DockerImageName TRINO_TEST_IMAGE = DockerImageName.parse("trinodb/trino:405");
    DockerImageName TRINO_PREVIOUS_VERSION_TEST_IMAGE = DockerImageName.parse("trinodb/trino:351");
}
