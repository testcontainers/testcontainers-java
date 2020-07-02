package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface PrestoTestImages {
    DockerImageName PRESTO_TEST_IMAGE = new DockerImageName("prestosql/presto:329");
    DockerImageName PRESTO_PREVIOUS_VERSION_TEST_IMAGE = new DockerImageName("prestosql/presto:328");
}
