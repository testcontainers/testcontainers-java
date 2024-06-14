package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface TimeplusTestImages {
    DockerImageName TIMEPLUS_PROTON_IMAGE = DockerImageName.parse("ghcr.io/timeplus-io/proton:latest");
    DockerImageName TIMEPLUS_IMAGE = DockerImageName.parse("ghcr.io/timeplus-io/proton:latest");
}
