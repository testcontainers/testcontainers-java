package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface DatabendTestImages {
    DockerImageName DATABEND_IMAGE = DockerImageName.parse("datafuselabs/databend:v1.2.615");
}
