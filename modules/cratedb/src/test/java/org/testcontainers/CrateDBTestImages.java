package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface CrateDBTestImages {
    DockerImageName CRATEDB_TEST_IMAGE = DockerImageName.parse("crate:5.2.5");
}
