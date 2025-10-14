package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface Db2TestImages {
    DockerImageName DB2_IMAGE = DockerImageName.parse("icr.io/db2_community/db2:11.5.8.0");
}
