package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface PrestoTestImages {
    DockerImageName PRESTO_TEST_IMAGE = DockerImageName.parse("prestosql/presto:329");
    DockerImageName PRESTO_PREVIOUS_VERSION_TEST_IMAGE = DockerImageName.parse("prestosql/presto:328");
}
