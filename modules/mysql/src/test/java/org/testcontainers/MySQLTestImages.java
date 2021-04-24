package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public class MySQLTestImages {
    public static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:5.7.22");
    public static final DockerImageName MYSQL_55_IMAGE = DockerImageName.parse("mysql:5.5");
    public static final DockerImageName MYSQL_56_IMAGE = DockerImageName.parse("mysql:5.6");
}
