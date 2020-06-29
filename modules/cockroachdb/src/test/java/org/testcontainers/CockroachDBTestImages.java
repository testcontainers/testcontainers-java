package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface CockroachDBTestImages {
    DockerImageName COCKROACHDB_IMAGE = new DockerImageName("cockroachdb/cockroach:v19.1.1");
}
