package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface MSSQLServerTestImages {
    DockerImageName MSSQL_SERVER_IMAGE = new DockerImageName("mcr.microsoft.com/mssql/server:2017-CU12");
}
