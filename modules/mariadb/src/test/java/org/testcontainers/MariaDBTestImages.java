package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface MariaDBTestImages {
    DockerImageName MARIADB_IMAGE = new DockerImageName("mariadb:10.3.6");
}
