package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface GaussDBTestImages {
    DockerImageName GAUSSDB_TEST_IMAGE = DockerImageName.parse("opengauss/opengauss:latest").asCompatibleSubstituteFor("gaussdb");
}
