package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface CockroachDBTestImages {
    DockerImageName COCKROACHDB_IMAGE = DockerImageName.parse("cockroachdb/cockroach:v22.2.3");
}
