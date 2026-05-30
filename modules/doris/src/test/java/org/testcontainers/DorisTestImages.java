package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface DorisTestImages {
    DockerImageName DORIS_IMAGE = DockerImageName.parse("apache/doris:3.1.0");
}
