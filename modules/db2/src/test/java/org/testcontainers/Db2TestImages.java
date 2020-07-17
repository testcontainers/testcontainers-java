package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface Db2TestImages {
    DockerImageName DB2_IMAGE = DockerImageName.parse("ibmcom/db2:11.5.0.0a");
}
