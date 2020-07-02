package org.testcontainers.spock;

import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.TestcontainersConfiguration;

interface SpockTestImages {
    DockerImageName MYSQL_IMAGE = new DockerImageName("mysql:5.7.22")
    DockerImageName POSTGRES_TEST_IMAGE = new DockerImageName("postgres:9.6.12")
    DockerImageName HTTPD_IMAGE = new DockerImageName("httpd:2.4-alpine")
    DockerImageName TINY_IMAGE = TestcontainersConfiguration.getInstance().getTinyDockerImageName()
}
