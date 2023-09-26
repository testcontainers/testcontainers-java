package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public class TiDBTestImages {

    public static final DockerImageName TIDB_IMAGE = DockerImageName.parse("pingcap/tidb:v6.1.0");
}
