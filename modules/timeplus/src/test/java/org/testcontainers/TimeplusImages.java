package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface TimeplusImages {
    DockerImageName TIMEPLUS_IMAGE = DockerImageName.parse("timeplus/timeplusd:2.3.3");
}
