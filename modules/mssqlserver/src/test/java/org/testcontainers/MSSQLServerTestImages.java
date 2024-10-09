package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface MSSQLServerTestImages {
    DockerImageName MSSQL_SERVER_IMAGE = DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-CU14-ubuntu-22.04");
}
