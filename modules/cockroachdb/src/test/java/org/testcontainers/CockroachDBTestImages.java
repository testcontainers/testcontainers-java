package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface CockroachDBTestImages {
    DockerImageName COCKROACHDB_IMAGE = DockerImageName.parse("cockroachdb/cockroach:v22.2.3");

    DockerImageName FIRST_COCKROACHDB_IMAGE_WITH_ENV_VARS_SUPPORT = DockerImageName.parse(
        "cockroachdb/cockroach:v22.1.0"
    );

    DockerImageName COCKROACHDB_IMAGE_WITH_ENV_VARS_UNSUPPORTED = DockerImageName.parse(
        "cockroachdb/cockroach:v21.2.17"
    );
}
