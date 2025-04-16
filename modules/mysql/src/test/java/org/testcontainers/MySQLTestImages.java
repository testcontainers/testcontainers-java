package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public class MySQLTestImages {

    public static final DockerImageName MYSQL_57_IMAGE = DockerImageName.parse("mysql:5.7.44");

    public static final DockerImageName MYSQL_80_IMAGE = DockerImageName.parse("mysql:8.0.36");

    public static final DockerImageName MYSQL_INNOVATION_IMAGE = DockerImageName.parse("mysql:8.3.0");

    public static final DockerImageName MYSQL_93_IMAGE = DockerImageName.parse("mysql:9.3.0");
}
